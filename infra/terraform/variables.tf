variable "kubeconfig_path" {
  description = "Path to the kubeconfig file used for kubernetes/helm providers"
  type        = string
  default     = "~/.kube/config"
}

variable "cluster_namespace" {
  description = "Namespace where Codex Pong workloads run"
  type        = string
  default     = "codexpong"
}

variable "argocd_chart_version" {
  description = "Version of the Argo CD Helm chart to install"
  type        = string
  default     = "7.6.12"
}
