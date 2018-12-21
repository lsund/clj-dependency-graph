# clj-dependency-graph

This program generates a dependency graph of a given clojure project. It zips
through the files and dependencies and generates a PNG image that looks
something like this:

![example-1](example-images/example-1.png)

Yes, it uses graphviz.

## Why?

Dependency graphs are useful, especially when developing prototypes. Before I
started this project, I spent a couple of minutes trying to find something
similar, but without success.

## Usage

```
=> lein run
usage: ./clj-dependency-graph OUTFILE SRCDIR

=> lein run test.png /path/to/my/clojure/project/src/
Generated Generated dependency graph: test.png
```

The second argument `SRCDIR` is the source directory to build the graph
from. The program filters clj files only, so if src has the example structure:

```
/path/to/my/clojure/project/src
├── clj
│   └── test
│       ├── app.clj
│       ├── config.clj
│       ├── core.clj
├── cljs
│   └── test
│       └── core.cljs

```

Then, only `app.clj`, `config.clj` and `core.clj` will be used for the graph.
