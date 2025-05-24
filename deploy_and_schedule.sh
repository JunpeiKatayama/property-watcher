#!/bin/bash

# 0. Javaアプリのfat jarビルド
./gradlew shadowJar

# 必要な変数を設定
PROJECT_ID="property-watcher-460606"
REGION="asia-northeast1"
IMAGE_NAME="property-watcher-job"
JOB_NAME="property-watcher-job"
SCHEDULER_NAME="property-watcher-job-scheduler"
CRON_SCHEDULE="0 9 * * *"  # 毎日9時に実行（日本時間）
TIME_ZONE="Asia/Tokyo"

# 1. Dockerイメージをビルド
docker build -t gcr.io/$PROJECT_ID/$IMAGE_NAME .

# 2. GCPにpush
docker push gcr.io/$PROJECT_ID/$IMAGE_NAME

# 3. Cloud Run Jobを作成（既存ならupdate）
if gcloud run jobs describe $JOB_NAME --region $REGION --project $PROJECT_ID > /dev/null 2>&1; then
  echo "Updating existing Cloud Run Job..."
  gcloud run jobs update $JOB_NAME \
    --image gcr.io/$PROJECT_ID/$IMAGE_NAME \
    --region $REGION \
    --project $PROJECT_ID
else
  echo "Creating new Cloud Run Job..."
  gcloud run jobs create $JOB_NAME \
    --image gcr.io/$PROJECT_ID/$IMAGE_NAME \
    --region $REGION \
    --memory 512Mi \
    --cpu 1 \
    --project $PROJECT_ID
fi

# 4. Cloud Schedulerで定期実行（既存ならupdate）
JOB_RESOURCE="projects/$PROJECT_ID/locations/$REGION/jobs/$JOB_NAME"

if gcloud scheduler jobs describe $SCHEDULER_NAME --location $REGION --project $PROJECT_ID > /dev/null 2>&1; then
  echo "Updating existing Cloud Scheduler job..."
  gcloud scheduler jobs update http $SCHEDULER_NAME \
    --location $REGION \
    --schedule "$CRON_SCHEDULE" \
    --time-zone "$TIME_ZONE" \
    --uri "https://$REGION-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/$PROJECT_ID/jobs/$JOB_NAME:run" \
    --http-method POST \
    --project $PROJECT_ID
else
  echo "Creating new Cloud Scheduler job..."
  gcloud scheduler jobs create http $SCHEDULER_NAME \
    --location $REGION \
    --schedule "$CRON_SCHEDULE" \
    --time-zone "$TIME_ZONE" \
    --uri "https://$REGION-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/$PROJECT_ID/jobs/$JOB_NAME:run" \
    --http-method POST \
    --project $PROJECT_ID
fi

echo "すべて完了しました！"
