# 3. Parsing ASN.1 Data

## Objective

Write an ASN.1 parser that is sufficient to handle private keys in the
EC and RSA format.

## Assignment details

No dependencies should be pulled-in by the project. An invocation of
`lein run <path/to/key>` should produce on standard output the data
representation of the parsed contents. An output consisting only of
using `clojure.pprint/pprint` on structured data would be perfectly
fine.

To simplify the process the following namespace can be used as a
starting point:

```clojure
(ns asn1.parser
  (:require [clojure.string  :as str]
            [clojure.java.io :as io]
            [clojure.pprint  :refer [pprint]])
  (:import java.io.RandomAccessFile
           java.nio.ByteBuffer
           javax.xml.bind.DatatypeConverter))

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

(defn parse-asn1
  [bb]
  ::nothing-parsed)
  
(defn -main [& args]
  (if-let [key-path (first args)]
    (pprint (parse-asn1 (base64-buffer key-path)))
	(binding [*out* *err*]
	  (println "no path given")
	  (System/exit 1))))
```

ASN.1 is based on encoding that optimizes for space, some interesting
links which could come in handy:

- [MSDN Documentation](https://msdn.microsoft.com/en-us/library/windows/desktop/bb540809(v=vs.85).aspx)
- [A layman's guide](http://luca.ntop.org/Teaching/Appunti/asn1.html)
- [Java ByteBuffer documentation](https://docs.oracle.com/javase/7/docs/api/java/nio/ByteBuffer.html)

Additionally, the `openssl` tool might come in handy to compare results:

    cat /path/to/key.pem | openssl asn1parse

## My Comments
* Due to the hard work to cope with bit-whise parsing and various hex-conversion I left implementation of "tail recursion" still open. I know about the stack limit for plain recursion - if relevant I can explain tail-recursion.
* I've put an asn.1 element oriented state machine as core element for parsing. I'm not shure whether that's the most elegant choice. But it works.
* Security as one of the very important topics for a parser is still open. So my implementation is not intended for production use :-). There are many aspects to consider:
  * The lookup in oid-encoding & tag-encoding is not safe against none existing keys. Input is not validated against such keys.
  * depends on how important oid lookup up is and whether this lookup happens against third party service or against a build in lookup map there should be a closer validation.
  * I've choosen my statemachine should parse any sequence of asn.1 elements. Not shure whether that should be allowed.
  * there is no real protection on decoding, length exeeding input stream length ...
  * every step of en-/de-coding should have close pre- and post-conditions
  * with the background of keys, key-chains, unsure algorithms, revoced certificates, certificates and intermediates I assume there is a security relevant semantic in parsing keys. A threat analysis seems to be a good idea.
* `javax.xml.bind.DatatypeConverter` is no longer included in jdk11, so I added a jdk11 profile having the compatibility libraray

### Just to remember
```
openssl genrsa -out src/test/resources/example-rsa.pem
openssl ecparam -genkey -name prime256v1 -noout -out src/test/resources/example-ec.pem

cat src/test/resources/example-rsa.pem | openssl asn1parse
cat src/test/resources/example-ec.pem | openssl asn1parse

lein with-profile jdk11 run src/test/resources/example-rsa.pem
lein with-profile jdk11 run src/test/resources/example-ec.pem
```

See also: 
* oc for openssl asn1parser - output: https://www.mkssoftware.com/docs/man1/openssl_asn1parse.1.asp
* base64->binary decoding https://cryptii.com/pipes/base64-to-binary
* Bouncycastle java impl: https://github.com/bcgit/bc-java
* Letsencrypt doc having the needed encodings: https://letsencrypt.org/docs/a-warm-welcome-to-asn1-and-der/
* rfc about x509: https://datatracker.ietf.org/doc/html/rfc5280
* rfc about RSA Cryptography Specifications: https://datatracker.ietf.org/doc/html/rfc8017
* oid resolving service: http://oid-info.com/get/1.2%20.840.10045.3.1.7
