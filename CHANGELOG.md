# Changelog

## 1.15.0

- Removido o som de morte do Ender Dragon quando ele desaparece por tempo.
- Morte normal por jogadores continua com som, mas em volume menor.
- Adicionada configuracao `sounds.dragon-death.normal-kill-volume`.
- Adicionada configuracao `sounds.dragon-death.stop-on-timeout`.

## 1.14.0

- Ajustado `/dragaoend setnpc` e `/dragaoend npc set` para salvar o armor stand no centro do bloco.
- Mantida a direcao do player ao salvar o NPC campeao.
- Nome acima do NPC campeao alterado para mostrar apenas o nick do jogador que matou o dragao.
- Config padrao do NPC campeao alterada para `champion-npc.name: "{player}"`.

## 1.13.0

- Adicionado NPC campeao em armor stand.
- NPC usa cabeca do jogador que matou o dragao.
- NPC equipa peitoral, calca e botas de netherite, com espada de netherite na mao.
- `/dragaoend npc set` salva o local fixo onde a estatua sera atualizada.

## 1.12.0

- Projeto renomeado para EnderDragon Spawner Plugin.
- JAR e repositorio atualizados para o novo nome.
- Adicionada migracao automatica da config antiga.
