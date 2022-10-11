(ns  todoot.core
  (:require [ajax.core :as ax]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tt]
            [dommy.core :refer-macros [sel1] :as dom]))

(def api-key "$2b$10$WeANikw9XvZVJZENB5lcOedfsqnvEEtJtPFzotoM7i3UhU/eYuP1S")

(def bin "634471970e6a79321e23f901")

(defn get-todos-from-api [cb]
  (ax/GET (str "https://api.jsonbin.io/v3/b/" bin)
    {:headers {:X-Access-Key api-key}
     :handler #(cb (get % "record"))
     :error-handler #(js/alert %)}))

(defn send-todos-to-api [json]
  (comment (ax/POST (str "https://api.jsonbin.io/v3/b/" bin)
             {:headers {:X-Access-Key api-key}
              :contentType "application/json"
              :data json
              :error-handler #(js/alert %)})))

(defrecord todo [title description place dueDate])

(defonce todos (atom #{}))

(defn append-todo! [new] (swap! todos conj new))

(defn selv [name] (-> name sel1 dom/value))
(defn parse-date  [date] (if (= date "") nil (t/date-time (js/Date. date))))
(defn read-todo []
  (->todo (selv :#inputTitle)
          (selv :#inputDescription)
          (selv :#inputPlace)
          (parse-date (selv :#inputDate))))

(declare deleter)

(declare save)

(defn make-deleter [item]
  (-> (dom/create-element :input)
      (dom/set-attr! :type "button", :value "⌦", :class "btn")
      (dom/listen! :click #(deleter item))))

(defn make-table-entry [child]
  (-> (dom/create-element :td)
      (dom/append! child)))

(defn make-row [nodes]
  (apply dom/append! (dom/create-element :tr) nodes))

(defn htmlize-todo [td]
  (->> td
       ((juxt :title
              #(tf/unparse (tf/formatter "yyyy-MM-dd") (:dueDate %))
              :place
              :description))
       (map dom/create-text-node)
       (cons (make-deleter td))
       (map make-table-entry)
       make-row))

(defn td-available [item]
  (and (let [search (selv :#inputSearch)]
         (or (= search "")
             (.includes (:description item) search)
             (.includes (:title item) search)))
       (if-let [lower-bound (parse-date (selv :#inputAfter))]
         (t/after? (:dueDate  item) lower-bound) true)
       (if-let [upper-bound (parse-date (selv :#inputBefore))]
         (t/before? (:dueDate  item) upper-bound) true)))

(defn update-todo-list []
  (let [root (sel1 :#todoListView)]
    (dom/clear! root)
    (->> @todos
         (filter td-available)
         (map htmlize-todo)
         (reduce dom/append! root))))

(defn add-todo []
  (append-todo! (read-todo))
  (update-todo-list)
  (save))

(defn deleter [item]
  (swap! todos disj item)
  (update-todo-list)
  (save))

(defn get-todos [] (->> @todos
                        (map #(update % :dueDate tt/to-date))
                        clj->js
                        (.stringify js/JSON)))

(defn save [] (send-todos-to-api (get-todos)))

(defn recover [x]
  (update (into {} (map (fn [[k v]] [(keyword k) v]) x)) :dueDate #(tt/from-date (js/Date. %))))

(defn load-todos! [new]
  (->> new
       js->clj
       (map recover)
       (into #{})
       (reset! todos)))

(defn load [] (get-todos-from-api #(do (load-todos! %) (update-todo-list))))
(defn init []
  (load)
  (dom/listen! js/window :load update-todo-list))
