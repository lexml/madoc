#!/bin/bash

# Script desatualizado!

# Não utilizar este script

set -e

json="node node_modules/json/lib/json.js"

versao=$($json -f ./projects/madoc-ui/package.json -a version)

nova_versao=$versao

if [ -z "$(git status --porcelain)" ]; then
  echo 'Git atualizado. Prosseguindo ...'
else
  echo 'Antes de executar esse script, é necessário que o servidor git esteja atualizado com as últimas modificações'
  exit -1
fi


while [ $nova_versao == $versao ]
  do
    read -e -p "Informe a versão: " -i $versao  nova_versao
  done


echo ------------
echo versão: $nova_versao
echo ------------

echo 'Atualizando o package.json com a nova versão'
read -p 'Tecle Enter para confirmar ou CTRL-C para abortar'

$json -I -f ./projects/madoc-ui/package.json -e "this.version='"$nova_versao"'"


echo 'Compilando a bilioteca e gerando o pacote NPM'

npm run package-madoc-ui

echo 'Publicando o pacote gerado no servidor Nexus'

npm run publish-madoc-ui

git commit projects/madoc-ui/package.json -m "atualizada a versão via script"

git push origin master

if [ -z "$(git status --porcelain)" ]; then
  echo 'Git atualizado com nova versão do package.json. Prosseguindo ...'
  git tag -a ""$nova_versao"" -m ""$nova_versao""
  git push origin $nova_versao
else
  echo 'Houve um erro inesperado. Abortando a geração da versão'
  exit -1
fi




