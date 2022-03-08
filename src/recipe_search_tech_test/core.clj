(ns recipe-search-tech-test.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.set :refer [intersection]]
            [criterium.core :as crit]
            [recipe-search-tech-test.heuristics.plurals :as plurals]
            [recipe-search-tech-test.heuristics.synonyms :as synonyms])
  (:gen-class))

;;; Matching tokens in the title of the recipe is much better than
;;; matching in the content.
(def ^:private ^:const TITLE-TOKEN-SCORE 50)
(def ^:private ^:const CONTENT-TOKEN-SCORE 1)

;;; Allow the use to specify the location of available recipe files as
;;; well as where the cached recipe data should be stored.
(def cli-config
  [["-r" "--recipes-directory DIRECTORY"
    :default "./resources/recipes"]
   ["-c" "--cache-file FILE"
    :default "./resources/recipe-cache.edn"]])

(defn sanitise
  "Lowercase a string and strip all non alpha/whitespace characters"
  [s]
  (-> s
      s/lower-case
      (s/replace #"[^a-z\s]" "")))

(defn scale-by
  "Multiply the vals of a frequencies map `m` by a constant factor `n`"
  [m n]
  (into {}
        (map (fn [[k v]]
               [k (* n v)]))
        m))

(def extract-filename
  (memoize
   (fn [file]
     (.getName file))))

;; The first line of each recipe appears to be the full title. This
;; hasn't been guaranteed, but does seem to be consistent, and makes
;; everything a lot prettier.
(def extract-title
  (memoize
   (fn [file]
     (-> file
         io/reader
         line-seq
         first))))

(defn parse-title
  [recipe-file]
  (-> recipe-file
      extract-title
      sanitise
      (s/split #"\s+")

      ;; Enrich title token list with heuristics
      (synonyms/apply-heuristic)
      (plurals/apply-heuristic)

      frequencies
      (scale-by TITLE-TOKEN-SCORE)))

(defn parse-content
  [recipe-file]
  (-> recipe-file
      slurp
      sanitise
      (s/split #"\s+")

      ;; Enrich content token list with heuristics
      (synonyms/apply-heuristic)
      (plurals/apply-heuristic)

      frequencies
      (scale-by CONTENT-TOKEN-SCORE)))

(defn cached?
  "Determine if a recipe has already been cached."
  [{:keys [cached-recipes]} recipe-file]
  (contains? cached-recipes (extract-filename recipe-file)))

(defn cache-recipe
  "Add a recipe to the in-memory cache if it's not already in there."
  [cache recipe-file]
  (if-not (cached? cache recipe-file)
    (let [filename (extract-filename recipe-file)
          title-data (parse-title recipe-file)
          content-data (parse-content recipe-file)
          recipe-data (merge-with + title-data content-data)]
      (-> (reduce (fn [c [token n]]
                    (-> c
                        (assoc-in [:token-data token filename] n)))
                  cache
                  recipe-data)
          (update :cached-recipes conj filename)))
    cache))

(defn cache-recipes
  "Traverse the recipe directory and ensure all recipes are in the
  cache."
  [cache recipes-directory]
  (reduce cache-recipe
          cache
          (filter #(.isFile %) (file-seq (io/file recipes-directory)))))

(defn search
  "Looking up a search term in the cache returns a maps of the recipes
  that contain that term with the score of that word for that recipe

  Terms score `TITLE_TOKEN_SCORE` points for each time they appear in
  the title of a recipe and `CONTENT_TOKEN_SCORE` points for each time
  they appear in the content of a recipe

  On my machine this function is executing in ~820µs for the search
  terms `[broccoli stilton soup]`

  `criterium.core/quick-bench`
  Evaluation count : 756 in 6 samples of 126 calls.
             Execution time mean : 817.964267 µs
    Execution time std-deviation : 24.174971 µs
   Execution time lower quantile : 801.154198 µs ( 2.5%)
   Execution time upper quantile : 859.739091 µs (97.5%)
                   Overhead used : 9.878030 ns"
  [cache search-terms]
  (->> search-terms
       (map (fn [term]
              (get-in cache [:token-data term])))
       (apply merge-with +)
       (sort-by second)
       reverse
       (take 10)))

(defn -main
  [& args]
  (let [{{:keys [recipes-directory cache-file]} :options search-terms :arguments}
        (parse-opts args cli-config)]

    (newline)
    (println "Building recipe cache...")

    (let [initial-cache (if (.exists (io/file cache-file))
                          (read-string (slurp cache-file))
                          {:cached-recipes #{}
                           :token-data {}})
          initial-recipe-count (count (:cached-recipes initial-cache))
          cache (cache-recipes initial-cache recipes-directory)
          new-recipe-count (- (count (:cached-recipes cache))
                              initial-recipe-count)]

      ;; Save any newly added recipes for next time
      (spit cache-file cache)
      (println (str "Recipe cache complete, loaded "
                    new-recipe-count
                    " new "
                    (if (= 1 new-recipe-count) "recipe" "recipes")))
      (newline)

      (while true
        (println "Enter your search terms:")
        (let [search-terms (s/split (sanitise (read-line)) #"\s+")
              _ (newline)
              results (time (search cache search-terms))]

          (newline)
          (println " --==| Maybe you should try |==--")
          (newline)
          (doseq [[recipe-filename score] results]
            (println (str score
                          " -- "
                          (extract-title (io/file (str recipes-directory "/" recipe-filename))))))
          (newline)
          (newline))))))
