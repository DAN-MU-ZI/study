FROM alpine:3.22

RUN apk add --no-cache 7zip

ENTRYPOINT ["7zz"]
CMD ["x", "/input/StackOverflow2010.7z", "-o/output", "-y"]

