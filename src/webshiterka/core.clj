(ns webshiterka.core
  (:require
   [mikera.image.core :as m]
   [mikera.image.colours :as c]
   [clojure.string :as string]))

(def palette (atom {:ctr 0 :used {}}))                  ; thread unsafe, bthere's one thread ¯\_(ツ)_/¯
(defn add-to-pallette [color]
  (let [{:keys [ctr used]} @palette]
    (if-let [present (get used color)]
      present
      (let [ctr' (inc ctr)
            key (str "x" ctr')
            used' (assoc used color key)]
        (reset! palette {:ctr ctr' :used used'})
        key))))
(defn make-palette [dict]
  (apply str (map (fn [[color tag]] (format "%s{background:%s;}\n" tag color)) dict)))
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
        id (add-to-pallette cstr)
        width (if (= count 1) ""
                  (format " style=\"width:%dem;\"" count))]
    (format "<%s%s></%s>" id width id)))

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
         println))
  (spit "palette.css" (make-palette (:used @palette))))
