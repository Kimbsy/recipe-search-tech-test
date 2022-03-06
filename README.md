## Recipe search

Searching for recipes really quickly.

### Requirements:

leiningen

### How to run:

```Bash
lein run
```

There are two optional arguments:

```Bash
# specifies the location of the input recipe text files
lein run --recipe-directory DIRECTORY
lein run --r DIRECTORY
```

``` Bash
# specifies the location to store the recipe-cache file
lein run --cache-file FILE
lein-run -c FILE
```

### How does it work?

We're parsing the collection of recipe files up front in order to
allow us to perform really quick searches. In the real world this tool
would be able to stay running and have new recipes added to it's
in-memory "recipe cache" (in this case it's just a hash-map).

The top-level keys of our recipe cache are the words (tokens) found
across all recipes, the associated values are then a map of recipe
file names to the score that token has in that recipe. Precedence is
given to recipes which have the token in their title (first line of
the recipe) as these are likely to be more relevant results.

```Clojure
{"cheese" {"cheese-on-toast.txt" 54    ;; cheese found in title once (50pts) and in body 4 times (4 points)
           "beans-on-toast.txt"   4    ;; cheese found in body 4 times (4 points)
           "garlic-bread.txt"     6}   ;; cheese found in body 6 times (6 points)

 "beans"  {"beans-on-toast.txt 53}     ;; beans found in title once (50 pts) and in body 3 times (3 points)

 ;; ... etc}
```

When searching for recipes for a given list of search terms we just
lookup those terms in the recipe cache and them merge the resulting
maps and adding up their scores. We can then sort and display the
results.

```Clojure
;; searching for "cheese beans"
{"beans-on-toast.txt" 57
 "cheese-on-toast.txt" 54
 "garlic-bread.txt" 6}
```

### Extra features:

The system also incorporates two heuristics in order to improve search
results, `Plurals` and `Synonyms`.

#### Plurals

Searching for the token `carrots` should also return recipes which
contain the token `carrot` and vice versa. We have a simple heuristic
which duplicates all tokens in a recipe adding or removing a trailing
`\s` character. A more complex pluralisation algorithm could be used,
but this covers a large majority of cases.

#### Synonyms

Searching for the token `pasta` should also return recipes which
contain the token `penne` as this is a type of pasta. We have included
a simple heuristic for specifying categories of ingredients which adds
the category tokens `pasta` or `cheese` when a recipe contains any
token which is specified as an instance of that category. This could
be extended as far as you like.

#### Etc.

More heuristics could easily be added to account for common
misspellings of words (e.g. maccaroni, daquiri, cesar salad), or
variance in US/UK English ingredient names (e.g. eggplant/aubergine,
argulua/rocket, cilantro/corriander).

### Is it fast?

Yes.

With a pre-built recipe cache, invoking the
`recipe-search-tech-test.core/search`
function in the REPL takes roughly 820µs. Printing the results to the
screen takes significantly longer.

The recipe cache itself takes about 3.2s to build, adding a new recipe
to an existing cache takes roughly 700µs.

Of course the search speed comes at the cost of memory usage. The
recipe-cache is about 24Mb, which is ~2.4 times larger than the total
of the input recipe files. This makes sense as we end up duplicating a
lot of the tokens, it could be mitigated by utilising a more cautious
pluralisation algorithm (though this could increase the cache build
time).
