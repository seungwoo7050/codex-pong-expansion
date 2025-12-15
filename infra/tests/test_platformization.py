import unittest
from pathlib import Path

import hcl2
import yaml

BASE_DIR = Path(__file__).resolve().parent.parent
K8S_BASE = BASE_DIR / "k8s" / "base"
KIND_OVERLAY = BASE_DIR / "k8s" / "overlays" / "kind"
TERRAFORM_MAIN = BASE_DIR / "terraform" / "main.tf"
GITOPS_APP = BASE_DIR / "gitops" / "argocd-application.yaml"


class PlatformizationManifestsTest(unittest.TestCase):
    """v1.4.0 플랫포마이제이션 산출물을 정적 검증한다."""

    def _load_yaml(self, path: Path):
        with path.open(encoding="utf-8") as handle:
            return [doc for doc in yaml.safe_load_all(handle) if doc]

    def _load_resource(self, path: Path, kind: str, name: str):
        for doc in self._load_yaml(path):
            if doc.get("kind") == kind and doc.get("metadata", {}).get("name") == name:
                return doc
        raise AssertionError(f"{path}에서 {kind}/{name} 리소스를 찾을 수 없습니다")

    def test_backend_probes_and_graceful_shutdown(self):
        deployment = self._load_resource(K8S_BASE / "backend-deployment.yaml", "Deployment", "backend")
        pod_spec = deployment["spec"]["template"]["spec"]
        container = pod_spec["containers"][0]

        readiness = container.get("readinessProbe", {})
        liveness = container.get("livenessProbe", {})
        lifecycle = container.get("lifecycle", {})

        self.assertEqual(readiness.get("httpGet", {}).get("path"), "/actuator/health/readiness")
        self.assertEqual(liveness.get("httpGet", {}).get("path"), "/actuator/health/liveness")
        self.assertIn("preStop", lifecycle)
        self.assertGreaterEqual(pod_spec.get("terminationGracePeriodSeconds", 0), 30)

    def test_kind_overlay_exposes_nodeports(self):
        backend_service = self._load_resource(
            KIND_OVERLAY / "backend-service-nodeport.yaml", "Service", "backend"
        )
        frontend_service = self._load_resource(
            KIND_OVERLAY / "frontend-service-nodeport.yaml", "Service", "frontend"
        )

        self.assertEqual(backend_service["spec"].get("type"), "NodePort")
        self.assertEqual(frontend_service["spec"].get("type"), "NodePort")
        self.assertEqual(backend_service["spec"]["ports"][0].get("nodePort"), 30080)
        self.assertEqual(frontend_service["spec"]["ports"][0].get("nodePort"), 30073)

    def test_terraform_defines_namespace_and_argocd(self):
        with TERRAFORM_MAIN.open(encoding="utf-8") as handle:
            config = hcl2.load(handle)
        resource_entries = {}
        for entry in config.get("resource", []):
            resource_entries.update(entry)

        self.assertIn("kubernetes_namespace", resource_entries)
        self.assertIn("helm_release", resource_entries)

        namespace_block = resource_entries["kubernetes_namespace"].get("codexpong")
        argocd_release = resource_entries["helm_release"].get("argocd")

        self.assertEqual(namespace_block["metadata"][0]["name"], "${var.cluster_namespace}")
        self.assertEqual(argocd_release["chart"], "argo-cd")
        self.assertIn("values", argocd_release)

    def test_gitops_application_targets_overlay(self):
        application = self._load_resource(GITOPS_APP, "Application", "codexpong-platform")
        source = application["spec"]["source"]

        self.assertEqual(source.get("path"), "infra/k8s/overlays/gitops")
        self.assertEqual(source.get("targetRevision"), "HEAD")
        self.assertIn("CreateNamespace=true", application["spec"].get("syncPolicy", {}).get("syncOptions", []))


if __name__ == "__main__":
    unittest.main()
