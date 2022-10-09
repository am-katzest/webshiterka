(ns webshiterka.core
  (:require
   [mikera.image.core :as m]
   [mikera.image.colours :as c]
   [clojure.string :as string]))

(defn split-into-pixels [image]
  (->> image
       m/get-pixels
       (mapv c/components-argb)
       (partition (m/width image))))

(defn htmlize-color [colors]
  (if (= 255 (last colors)) (apply  format "#%02x%02x%02x" (take 3 colors))
      (apply  format "#%02x%02x%02x%02x" colors)))

(defn make-pixel [[count colors]]
  (let [cstr (htmlize-color colors)
        width (if (= count 1) ""
                  (format "width:%dem;" count))]
    (format "<div style=\"background:%s;%s\"></div>" cstr width)))

(defn transform-pixel-array-into-html [arr]
  (apply str (mapcat (fn [row] (concat ["<div id=\"newline\"></div>"] (map make-pixel row))) arr)))

(defn group-pixels [row]
  (->> row
       (partition-by identity)
       (map (juxt count first))))

(defn image2html [filename]
  (->> filename
       java.io.File.
       m/load-image
       split-into-pixels
       (map group-pixels)
       transform-pixel-array-into-html))

(defn mod-template [html [placeholder image]]
  (string/replace html placeholder
                  (image2html image)))

(defn -main
  [template & replacements]
  (let [html (slurp template)]
    (->> replacements
         (partition 2)
         (reduce mod-template html)
         println)))
