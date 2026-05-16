## 1. Salvage useful docs

- [ ] 1.1 Read `config/k8s/vanilla/api-gateway/ingress/ingress_README.md`. Identify content that is still useful (routing detail, path-rewriting nuances).
- [ ] 1.2 Read `config/k8s/vanilla/kafka/kafka_README.md`. Identify any unique notes (zookeeper-mode rationale, etc.).
- [ ] 1.3 Decide a target home for salvaged content: append to `config/k8s/helm/helm_README.md` or create `config/k8s/INGRESS.md` and `config/k8s/KAFKA.md`. Prefer keeping things in the helm tree.
- [ ] 1.4 Copy salvaged text in; rewrite where it referenced vanilla file paths.

## 2. Delete

- [ ] 2.1 `git rm -r config/k8s/vanilla/`.
- [ ] 2.2 `git rm -r config/k8s/_deployment-scripts/vanilla/`.

## 3. Update root README

- [ ] 3.1 Replace `./config/k8s/vanilla/api-gateway/ingress/ingress_README.md` link with the new helm-side INGRESS doc path.
- [ ] 3.2 Replace `./config/k8s/vanilla/kafka/kafka_README.md` link similarly.
- [ ] 3.3 Update the "How it works" section's reference to "ingress controller" if the wording suggested vanilla.

## 4. Update deployment_README.md

- [ ] 4.1 Open `config/k8s/_deployment-scripts/deployment_README.md`.
- [ ] 4.2 Remove the 8 `kubectl apply -f vanilla/...` commands.
- [ ] 4.3 Remove the "Clearing" section if it targets vanilla resources only.
- [ ] 4.4 Add a "Helm-only deployment" headline if not already prominent.

## 5. Update k8s_README.md

- [ ] 5.1 Review and remove any "vanilla" mentions or "vanilla vs helm" comparisons.

## 6. Verify

- [ ] 6.1 `grep -rn 'vanilla' . --exclude-dir=.git` returns no source or doc hits (skill names like `hexagonal-architecture` are unrelated, but verify no `k8s/vanilla` references remain anywhere).
- [ ] 6.2 README markdown links all resolve (`grep -oE '\(./[^)]+\)' README.md | xargs -I{} test -e {}` or open in VS Code preview).
- [ ] 6.3 Helm deploy still works end-to-end via the helm-deploy script.
