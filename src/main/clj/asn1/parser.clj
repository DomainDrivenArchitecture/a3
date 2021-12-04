(ns asn1.parser
  (:require [clojure.string  :as str]
            [clojure.java.io :as io]
            [clojure.pprint  :refer [pprint]])
  (:import java.io.RandomAccessFile
           java.nio.ByteBuffer
           javax.xml.bind.DatatypeConverter))

(def oid-encoding {"2A8648CE3D317" {:oid "1.2 .840.10045.3.1.7"
                                    :name "prime256v1"}})

(def asn1-encoding {0x02 {:name "INTEGER"
                          :header-length 2
                          :type :prim}
                    0x03 {:name "BIT STRING"
                          :header-length 2
                          :type :prim}
                    0x04 {:name "OCTET STRING"
                          :header-length 2
                          :type :prim}
                    0x06 {:name "OBJECT"
                          :header-length 2
                          :type :prim}
                    0x30 {:name "SEQUENCE"
                          :alt-name "sequence of"
                          :header-length 2
                          :type :cons}
                    -95 {:name "cont"
                         :header-length 2
                         :type :cons}
                    -96 {:name "cont"
                         :header-length 2
                         :type :cons}})

(defn base64-extract
  [path]
  (reduce str "" (remove #(str/starts-with? % "----") (line-seq (io/reader path)))))

(defn base64-bytes
  [path]
  (let [b64-str ^String (base64-extract path)]
    (DatatypeConverter/parseBase64Binary b64-str)))

(defn base64-buffer
  [path]
  (ByteBuffer/wrap (base64-bytes path)))

(defn read-tag [& bs]
  (if-let [tag (first bs)]
    tag
    (throw (ex-info "tag not found" {}))))

(defn read-length [& bs]
  (if-let [length (second bs)]
    length
    (throw (ex-info "length not found" {}))))

(defn decode-value [elem value]
  (let [hex-val (str/join (map #(format "%X" %) value))]
    (println hex-val)
    (if (= (:name  elem) "OBJECT")
      (get oid-encoding hex-val)
      hex-val)))

(defn decode-inner [state & bs]
  (if (not-empty bs)
    (let [tag (apply read-tag bs)
          length (apply read-length bs)
          elem (get asn1-encoding tag)
          type (:type elem)
          output {:offset (:offset state)
                  :depth (:depth state)
                  :header-length (:header-length elem)
                  :value-length length
                  type (:name elem)}
          offset-inc (:header-length elem)
          tail (nthrest bs offset-inc)
          result (conj (:result state)
                       output)]
      (println bs)
      (println output)
      (case type
        :cons
        (assoc state :result
               (concat result
                       (:result
                        (apply decode-inner
                               (assoc state
                                      :offset (+ (:offset state) offset-inc)
                                      :depth (inc (:depth state)))
                               tail))))
        :prim
        (let [split-value (split-at length tail)
              value (first split-value)
              tail (second split-value)
              offset-inc (+ (:header-length elem) length)
              output (assoc output
                            :value (decode-value elem value))
              result (conj (:result state)
                           output)]
          (assoc state :result
                 (concat result
                         (:result
                          (apply decode-inner
                                 (assoc state
                                        :offset (+ (:offset state) offset-inc))
                                 tail)))))
        (throw (ex-info "unknown type" {}))))
    (do
      (println state)
      state)))

(defn decode [& bs]
  (:result
   (apply decode-inner {:offset 0
                        :depth 0
                        :result []}
          bs)))

(defn my-parse [bs]
  (->> bs
      (map )))

(defn parse-asn1
  [bb]
  ::nothing-parsed)

(defn -main [& args]
  (if-let [key-path (first args)]
    (pprint (parse-asn1 (base64-buffer key-path)))
    (binding [*out* *err*]
      (println "no path given")
      (System/exit 1))))