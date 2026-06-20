# Como aplicar este pacote no seu repositório

Este `.zip` contém **apenas os arquivos novos/alterados de DevOps**
(Terraform, Dockerfiles, docker-compose.yml, application.yml adaptados para
PostgreSQL, pom.xml adaptados, DEPLOY.md). Ele **não** contém as classes Java
de domínio (entity, service, controller, repository, dto, event) — essas já
existem no seu repositório `Deploy` e não precisam mudar.

## Passo a passo

```bash
# 1. Clone seu repositório (se ainda não tiver localmente)
git clone https://github.com/miguel-oliveira-silva/Deploy.git
cd Deploy

# 2. Extraia o conteúdo deste zip POR CIMA do repositório
#    (ele vai sobrescrever pom.xml e application.yml dos 4 serviços,
#     e adicionar Dockerfile, docker-compose.yml, terraform/, DEPLOY.md)
unzip -o /caminho/para/markovitz-devops.zip -d /tmp/markovitz-devops-extract
cp -r /tmp/markovitz-devops-extract/markovitz-devops/. .

# 3. Confira o que mudou
git status
git diff --stat

# 4. Commit e push
git add -A
git commit -m "feat(devops): adiciona Terraform, Docker e adapta servicos para PostgreSQL"
git push origin main
```

## Alternativa: usar o histórico de commits já pronto

Se preferir importar os 4 commits organizados que já vêm prontos dentro deste
pacote (em vez de um commit único "tudo de uma vez"), você pode usar o
`.git` embutido no zip como um remoto temporário:

```bash
cd Deploy
git remote add pacote-devops /tmp/markovitz-devops-extract/markovitz-devops
git fetch pacote-devops
git merge pacote-devops/main --allow-unrelated-histories
git remote remove pacote-devops
git push origin main
```

Isso preserva as 4 mensagens de commit temáticas (banco de dados, Docker,
Terraform, documentação) no seu histórico.

## Depois do push: rodando o deploy

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# edite terraform.tfvars com suas senhas

terraform init
terraform apply
```

Veja `DEPLOY.md` na raiz do projeto para a documentação completa.
