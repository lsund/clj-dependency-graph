# clj-dependency-graph

This program generates a dependency graph of a given clojure project. It zips
through the files and dependencies and generates a PNG image that looks
something like this:

![example-1](example-images/example-1.png)

Yes, it uses graphviz.

## Why?

No real reason

## Usage

```
# You can run it like this
lein run test.png /path/to/my/clojure/project
```
