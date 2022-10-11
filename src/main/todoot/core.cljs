(ns  todoot.core
  (:require [ajax.core :as ax]))

(def api-key "$2b$10$WeANikw9XvZVJZENB5lcOedfsqnvEEtJtPFzotoM7i3UhU/eYuP1S")
(def bin "634471970e6a79321e23f901")
(defn get-todos-from-api [cb]
  (ax/GET (str "https://api.jsonbin.io/v3/b/" bin)
    {:headers {:X-Access-Key api-key}
     :handler #(cb (get % "record"))
     :error-handler #(js/alert %)}))

(defrecord todo [title description place dueDate])

(defonce todos (atom #{}))

(defn append-todo! [new] (swap! todos conj new))
(defn get-html-value [name] (.-value (.getElementById js/document name)))
(defn read-todo []
  (let [[title desc place date]
        (map get-html-value
             ["inputTitle" "inputDescription" "inputPlace" "inputDate"])]
    (->todo title desc place (js/Date. date))))

(declare deleter)

(defn make-deleter [item]
  (let [new-button (.createElement js/document "input")]
    (set! (.-type new-button) "button")
    (doto new-button
      (.setAttribute  "value" "⌦")
      (.addEventListener "click" #(deleter item)))
    new-button))

(defn htmlize-todo [td]
  (let [ret (.createElement js/document "div")
        deleter (make-deleter td)    ; good enough, there should be id's but uhh
        content (.createTextNode js/document (str (:title td) " " (:description td) (into {} td)))]
    (.appendChild ret deleter)
    (.appendChild ret content)
    ret))

(defn kill-all-children [elem]
  (when-let [child (.-lastChild elem)]
    (.removeChild elem child)
    (recur elem)))
(defn td-available [item search]
  (or (= search "")
      (.includes (:description item) search)
      (.includes (:title item) search)))

(defn update-todo-list []
  (let [root (.getElementById js/document "todoListView")
        search-str (get-html-value "inputSearch")]
    (kill-all-children root)
    (doseq [td (filter #(td-available % search-str) @todos)]
      (.appendChild root (htmlize-todo td)))))

(defn add-todo []
  (append-todo! (read-todo))
  (update-todo-list))

(defn deleter [item]
  (swap! todos disj item)
  (update-todo-list))

(defn get-todos [] (->> @todos
                        clj->js
                        (.stringify js/JSON)))

(defn keywordize [x]
  (into {} (map (fn [[k v]] [(keyword k) v]) x)))

(defn load-todos! [new]
  (->> new
       js->clj
       (map keywordize)
       (into #{})
       (reset! todos)))
(defn init [] (get-todos-from-api #(do (load-todos! %) (update-todo-list)))
  (.addEventListener js/window "load" update-todo-list))
