lein uberjar
aws s3 sync resources/public/ s3://hiiop-prod
heroku git:remote --app hiiop
heroku maintenance:on --app hiiop
DATABASE_URL="$(heroku config:get "DATABASE_URL" --app hiiop)?sslmode=require" lein migratus migrate
HEROKU_APP="hiiop" lein heroku deploy-uberjar
heroku maintenance:off --app hiiop