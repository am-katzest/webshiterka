(ns webshiterka.core
  (:require
   [mikera.image.core :as m]
   [mikera.image.colours :as c]))
(defn group-pixels [row] (->> row (partition-by identity) (map (juxt first count))))

(defn split-into-pixels [image]
  (->> image
       m/get-pixels
       (mapv c/components-argb)
       (partition (m/width image))))
(def space "<div id=\"hacky\"/>")
(defn htmlize-color [colors] (apply  format "#%02x%02x%02x%02x" colors))
(defn make-pixel [colors]
  (format "<div id=\"square\" style=\"background:%s;\"></div>" (htmlize-color colors)))
(defn transform-pixel-array-into-html [arr]
  (apply str (mapcat (fn [row] (concat [space] (map make-pixel row))) arr)))

(defn -main
  [in]
  (->> in
       java.io.File.
       m/load-image
       split-into-pixels
       transform-pixel-array-into-html
       (println)))
