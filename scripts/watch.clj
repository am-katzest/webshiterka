(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'todoot.core
   :output-to "out/todoot.js"
   :output-dir "out"})
