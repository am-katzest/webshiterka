(ns  todoot.core
  (:require
   [cljs-time.core :as t]
   [cljs-time.format :as tf]
   [cljs-time.coerce :as tt]
   [dommy.core :refer-macros [sel1] :as dom]
   [todoot.bin :as bin]))

;; code is a little mangled

(declare delete-todo)

(declare save)

;; utils
(defn selv [x] (dom/value (sel1 x)))

(defn parse-date  [date] (if (= date "") nil (t/date-time (js/Date. date))))

;; data

(defrecord todo [title description place dueDate])

(defonce todos (atom #{}))

(defn append-todo! [new] (swap! todos conj new))

(defn read-todo []
  (->todo (selv :#inputTitle)
          (selv :#inputDescription)
          (selv :#inputPlace)
          (parse-date (selv :#inputDate))))

;; displaying

;; ;; table

(defn make-deleter [item]
  (-> (dom/create-element :input)
      (dom/set-attr! :type "button", :value "⌦", :class "btn")
      (dom/listen! :click #(delete-todo item))))

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

(defn is-avialable? [item]
  (js/alert (selv :#inputSearch))
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
         (filter is-avialable?)
         (map htmlize-todo)
         (reduce dom/append! root))))

;; modifying of todo list

(defn add-todo []
  (append-todo! (read-todo))
  (update-todo-list)
  (save))

(defn delete-todo [item]
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
  (dom/listen! js/window :load update-todo-list))
