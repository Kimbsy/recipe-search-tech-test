(ns recipe-search-tech-test.heuristics.plurals)

;;;; This ns represents a relatively naiive but very effective
;;;; pluralisation heuristic.

;;;; We want a search for `sausages` to be able to return the recipe
;;;; `beef-sausage-roll.txt` and we want a search for `sausage` to
;;;; return the recipe `sausages-with-onion-gravy.txt`.

;;;; In order to achieve this, we can duplicate tokens, modifying them
;;;; based on whether they end in `s` as this represents the most
;;;; common form of pluralisation.

;;; `sausage` => `sausages`
;;; `eggs`    => `egg`

;;;; We will have some odd results where non-ingredient tokens get
;;;; modified, or when ingredients that naturally end in `s` get
;;;; incorrectly depluralised, but the original token will still be
;;;; there, so any sensible search should be largely unaffected.

;;; `choppped` => `choppeds`
;;; `cress`    => `cres`

(defn ends-in-s?
  [s]
  (= \s (last s)))

(defn pluralise
  [s]
  (str s "s"))

(defn depluralise
  [s]
  (apply str (butlast s)))

(defn apply-heuristic
  [tokens]
  (reduce (fn [acc token]
            (if (ends-in-s? token)
              (conj acc token (depluralise token))
              (conj acc token (pluralise token))))
          []
          tokens))
