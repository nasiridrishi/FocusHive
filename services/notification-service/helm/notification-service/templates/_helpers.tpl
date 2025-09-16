{{/*
Expand the name of the chart.
*/}}
{{- define "notification-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "notification-service.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "notification-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "notification-service.labels" -}}
helm.sh/chart: {{ include "notification-service.chart" . }}
{{ include "notification-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: focushive
app.kubernetes.io/component: notification-service
{{- end }}

{{/*
Selector labels
*/}}
{{- define "notification-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "notification-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "notification-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "notification-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the secret to use
*/}}
{{- define "notification-service.secretName" -}}
{{- if .Values.secrets.existingSecret }}
{{- .Values.secrets.existingSecret }}
{{- else }}
{{- printf "%s-secrets" (include "notification-service.fullname" .) }}
{{- end }}
{{- end }}

{{/*
Create the name of the configmap to use
*/}}
{{- define "notification-service.configMapName" -}}
{{- if .Values.config.existingConfigMap }}
{{- .Values.config.existingConfigMap }}
{{- else }}
{{- printf "%s-config" (include "notification-service.fullname" .) }}
{{- end }}
{{- end }}

{{/*
Create a default database host name when PostgreSQL is enabled as a dependency
*/}}
{{- define "notification-service.databaseHost" -}}
{{- if .Values.postgresql.enabled }}
{{- printf "%s-postgresql" (include "notification-service.fullname" .) }}
{{- else }}
{{- .Values.config.database.host }}
{{- end }}
{{- end }}

{{/*
Create a default Redis host name when Redis is enabled as a dependency
*/}}
{{- define "notification-service.redisHost" -}}
{{- if .Values.redis.enabled }}
{{- printf "%s-redis-master" (include "notification-service.fullname" .) }}
{{- else }}
{{- .Values.config.redis.host }}
{{- end }}
{{- end }}

{{/*
Create a default RabbitMQ host name when RabbitMQ is enabled as a dependency
*/}}
{{- define "notification-service.rabbitmqHost" -}}
{{- if .Values.rabbitmq.enabled }}
{{- printf "%s-rabbitmq" (include "notification-service.fullname" .) }}
{{- else }}
{{- .Values.config.rabbitmq.host }}
{{- end }}
{{- end }}

{{/*
Create image pull policy
*/}}
{{- define "notification-service.imagePullPolicy" -}}
{{- if .Values.image.tag }}
{{- if eq .Values.image.tag "latest" }}
Always
{{- else }}
IfNotPresent
{{- end }}
{{- else }}
{{- if .Values.image.pullPolicy }}
{{- .Values.image.pullPolicy }}
{{- else }}
IfNotPresent
{{- end }}
{{- end }}
{{- end }}

{{/*
Create image reference
*/}}
{{- define "notification-service.image" -}}
{{- if .Values.image.registry }}
{{- printf "%s/%s:%s" .Values.image.registry .Values.image.repository (.Values.image.tag | default .Chart.AppVersion) }}
{{- else }}
{{- printf "%s:%s" .Values.image.repository (.Values.image.tag | default .Chart.AppVersion) }}
{{- end }}
{{- end }}

{{/*
Create security context
*/}}
{{- define "notification-service.securityContext" -}}
{{- with .Values.securityContext }}
{{- toYaml . }}
{{- end }}
{{- end }}

{{/*
Create pod security context
*/}}
{{- define "notification-service.podSecurityContext" -}}
{{- with .Values.podSecurityContext }}
{{- toYaml . }}
{{- end }}
{{- end }}

{{/*
Validate required values
*/}}
{{- define "notification-service.validateValues" -}}
{{- if not .Values.secrets.database.username }}
{{- fail "secrets.database.username is required" }}
{{- end }}
{{- if not .Values.secrets.database.password }}
{{- fail "secrets.database.password is required" }}
{{- end }}
{{- if not .Values.secrets.email.awsSesUsername }}
{{- fail "secrets.email.awsSesUsername is required" }}
{{- end }}
{{- if not .Values.secrets.email.awsSesPassword }}
{{- fail "secrets.email.awsSesPassword is required" }}
{{- end }}
{{- end }}