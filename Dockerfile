FROM clojure:lein-2.8.1-alpine

RUN apk update && \
    apk upgrade && \
    apk add git && \
    apk add sassc

ADD . /hiiop/

WORKDIR /hiiop