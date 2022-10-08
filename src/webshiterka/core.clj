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

(def space "<div id=\"hacky\"/>")

(defn htmlize-color [colors] (apply  format "#%02x%02x%02x%02x" colors))

(defn make-pixel [[count colors]]
  (let [cstr (htmlize-color colors)
        width (if (= count 1) ""
                  (format "width:%dem;" count))]
    (format "<div id=\"square\" style=\"background:%s;%s\"></div>" cstr width)))

(defn transform-pixel-array-into-html [arr]
  (apply str (mapcat (fn [row] (concat [space] (map make-pixel row))) arr)))

(defn group-pixels [row]
  (->> row
       (partition-by identity)
       (map (juxt count first))))

(defn -main
  [template in]
  (let [html (slurp template)
        insert-into-template
        #(string/replace html #"%%%" %1)]
    (->> in
         java.io.File.
         m/load-image
         split-into-pixels
         (map group-pixels)
         transform-pixel-array-into-html
         insert-into-template
         println)))
