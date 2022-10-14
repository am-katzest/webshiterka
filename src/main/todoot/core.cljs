(ns  todoot.core
  (:require
   [cljs-time.core :as t]
   [cljs-time.format :as tf]
   [cljs-time.coerce :as tt]
   [todoot.bin :as bin]))

(def $ (js* "$")) ;; jquery is imported via html

;; code is a little mangled

(declare delete-todo!)

(declare save)

;; utils
(defn v$ [x] (.val ($ x)))

(defn parse-date  [date] (if (= date "") nil (t/date-time (js/Date. date))))

(defn d$ [x] (parse-date (v$ x)))

(defn append! [root children]
  (doseq [c children]
    (.append root c))
  root)

;; data

(defrecord todo [title description place dueDate])

(defonce todos (atom #{}))

(defn read-todo []
  (->todo (v$ "#inputTitle")
          (v$ "#inputDescription")
          (v$ "#inputPlace")
          (d$ "#inputDate")))

;; displaying

;; ;; table

(defn make-deleter [item]
  (doto ($ "<input>")
    (.attr "type" "button")
    (.attr "value" "⌦")
    (.attr "class" "btn")
    (.on "click" #(delete-todo! item))))

(defn make-table-entry [child]
  (-> ($ "<td>")
      (.append child)))

(defn make-row [nodes]
  (append! ($ "<tr>") nodes))

(defn htmlize-todo [td]
  (->> td
       ((juxt :title
              #(tf/unparse (tf/formatter "yyyy-MM-dd") (:dueDate %))
              :place
              :description))
       (cons (make-deleter td))
       (map make-table-entry)
       make-row))

(defn is-avialable? [item]
  (let [Search (v$ "#inputSearch")
        search (.toLowerCase Search)
        matches #(.includes (.toLowerCase %) search)
        lower-bound (d$ "#inputAfter")
        upper-bound (d$ "#inputBefore")
        date (:dueDate item)]
    (and (or (= search "")
             (matches (:description item))
             (matches (:title item)))
         (or (nil? lower-bound)
             (t/after? date lower-bound))
         (or (nil? upper-bound)
             (t/before? date upper-bound)))))

(defn update-todo-list []
  (let [root ($ "#todoListView")]
    (.empty root)
    (->> @todos
         (filter is-avialable?)
         (map htmlize-todo)
         (append! root))))

;; modifying of todo list

(defn add-todo! [item]
  (swap! todos conj item)
  (update-todo-list)
  (save))

(defn delete-todo! [item]
  (swap! todos disj item)
  (update-todo-list)
  (save))

;; json handling
(defn todos->json [x]
  (->> x
       (map #(update % :dueDate tt/to-date))
       clj->js
       (.stringify js/JSON)))

(defn recover-todo-post-jsonification [x]
  (update (into {} (map (fn [[k v]] [(keyword k) v]) x))
          :dueDate parse-date))

(defn json->todos [new]
  (->> new
       js->clj
       (map recover-todo-post-jsonification)
       (into #{})))

;; bin <-> todos atom
(defn save [] (-> @todos todos->json bin/send-todos-to-api))

(defn load [] (bin/get-todos-from-api
               #(do (reset! todos (json->todos %))
                    (update-todo-list))))

;; is called at the beginning
(defn init []
  (load)
  ($ (fn []
       (.on ($ "#addTodo") "click" #(add-todo! (read-todo)))
       (.on ($ "#inputSearch") "keyup" update-todo-list)
       (.on ($ "#inputBefore") "change" update-todo-list)
       (.on ($ "#inputAfter") "change" update-todo-list)
       (update-todo-list))))
