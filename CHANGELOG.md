# Changelog

## 1.16.0

- Corrigida a acentuação dos textos em português no README, guia, config padrão e mensagens do plugin.

## 1.15.0

- Removido o som de morte do Ender Dragon quando ele desaparece por tempo.
- Morte normal por jogadores continua com som, mas em volume menor.
- Adicionada configuração `sounds.dragon-death.normal-kill-volume`.
- Adicionada configuração `sounds.dragon-death.stop-on-timeout`.

## 1.14.0

- Ajustado `/dragaoend setnpc` e `/dragaoend npc set` para salvar o armor stand no centro do bloco.
- Mantida a direção do player ao salvar o NPC campeão.
- Nome acima do NPC campeão alterado para mostrar apenas o nick do jogador que matou o dragão.
- Config padrão do NPC campeão alterada para `champion-npc.name: "{player}"`.

## 1.13.0

- Adicionado NPC campeão em armor stand.
- NPC usa cabeça do jogador que matou o dragão.
- NPC equipa peitoral, calça e botas de netherite, com espada de netherite na mão.
- `/dragaoend npc set` salva o local fixo onde a estátua será atualizada.

## 1.12.0

- Projeto renomeado para EnderDragon Spawner Plugin.
- JAR e repositório atualizados para o novo nome.
- Adicionada migração automática da config antiga.
