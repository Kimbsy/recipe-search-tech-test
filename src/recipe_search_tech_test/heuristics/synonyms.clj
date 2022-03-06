(ns recipe-search-tech-test.heuristics.synonyms)

;;;; This ns represents a basic synonym heuristic.

;;;; When searching for generalised categories of ingredients you want
;;;; to try to ensure that specific instances are included. We want a
;;;; search for `pasta` to return `squash-rosemary-tagliatelle.txt`.

;;;; For brevity we're just going to do pasta a cheese, as these are
;;;; very common ingredients which have a lot of interchangeability.

;;;; This heuristic could be extended to include categories of
;;;; macronutrients such as enriching any token list containing `beef`
;;;; or `pork` with `protein`, this would allow people to search for
;;;; meals high in `protein`, `carbophydrates`, `fibre`, etc.

(def synonyms
  {"pasta" #{"casarecce" "cavatappi" "penne" "reginette" "anellini" "lasagne"
             "cappalletti" "pipette" "farfalline" "lasagna" "anelli"
             "conchiglie" "ruote" "vermicelli" "mafalda" "fideo" "ziti"
             "fusilli" "rotelle" "thin" "ravioli" "ditalini" "pappardelle"
             "tortellini" "pipe" "orzo" "linguine" "pastina" "tubini"
             "pepe" "riccioli" "alphabet" "tripolini" "radiatori" "cavatelli"
             "bucatini" "elbow" "gemelli" "gigli" "rocchetti" "rotini"
             "manicotti" "mostaccioli" "tortiglioni" "farfalle"
             "tagliatelle" "campanelle" "spaghetti" "orecchiette" "rigate"
             "rigatoni" "fettuccine"}
   "cheese" #{"muenster" "epoisses" "mozzarella" "stilton" "roquefort" "edam"
              "feta" "mascarpone" "provolone" "gouda" "cheddar" "taleggio"
              "manchego" "montagnolo" "halloumi" "ricotta" "brie" "reblochon"
              "cambozola" "gruyere" "gorgonzola" "camembert" "parmigiano"}})

(defn apply-heuristic
  [tokens]
  (reduce (fn [acc token]
            (let [categories (->> synonyms
                                  (map (fn [[category instances]]
                                         (when (contains? instances token)
                                           category)))
                                  (remove nil?))]
              (apply conj acc token categories)))
          []
          tokens))
