# EnderDragon Spawner Plugin - Guia de Uso

Plugin para Paper 1.21.11 que spawna um Dragao do End automaticamente no End, com tempo para matar, boss bar, top dano, recompensa para top 1 e protecao para o dragao nao quebrar blocos.

## Instalacao

1. Coloque o arquivo `EnderDragon-Spawner-Plugin-1.13.0-paper-1.21.11.jar` na pasta `plugins/`.
2. Reinicie o servidor.
3. O plugin vai criar a config em:

```text
plugins/EnderDragonSpawnerPlugin/config.yml
```

4. Configure por comandos ou edite a config pela host.

Permissao principal:

```text
enddragonsafe.admin
```

Por padrao, OP ja tem permissao.

Comando principal:

```text
/dragaoend
```

Aliases:

```text
/eds
/enddragonspawn
/dragao
```

## Comandos

### Ajuda

```text
/dragaoend help
```

Mostra a lista basica de comandos dentro do servidor.

### Status

```text
/dragaoend status
```

Mostra se o plugin esta ativo, mundo/coordenada do spawn, intervalo, tempo para matar, proximo spawn, boss bar, protecao de blocos e recompensa do top 1.

### Configurar intervalo de spawn

```text
/dragaoend setinterval <tempo>
```

Exemplos:

```text
/dragaoend setinterval 3h
/dragaoend setinterval 5h
/dragaoend setinterval 30m
/dragaoend setinterval 90m
/dragaoend setinterval 1.5h
```

Esse intervalo define de quanto em quanto tempo o dragao nasce. Se o dragao sumir por tempo, ele so volta no proximo ciclo configurado.

### Configurar tempo para matar

```text
/dragaoend setkilltime <tempo>
```

Exemplos:

```text
/dragaoend setkilltime 15m
/dragaoend setkilltime 30m
/dragaoend setkilltime 1h
```

Se os jogadores nao matarem o dragao nesse tempo, ele morre pela animacao vanilla do Minecraft, nao dropa XP, nao dropa item, nao gera ovo/portal pelo plugin, e aparece no chat apenas que ele desapareceu porque o tempo acabou.

### Configurar coordenadas do spawn

```text
/dragaoend setcoords <x> <y> <z> [mundo]
```

Exemplo:

```text
/dragaoend setcoords 0 80 0 world_the_end
```

O mundo precisa ser do tipo `THE_END`.

### Usar sua posicao atual como spawn

```text
/dragaoend sethere
```

Use estando dentro do End. O plugin salva sua posicao atual como local de nascimento do dragao.

### Spawnar o dragao manualmente

```text
/dragaoend nascer
```

Tambem funciona:

```text
/dragaoend spawn
```

Se ja existir um Ender Dragon vivo no mundo configurado, o plugin pode impedir outro spawn dependendo da config.

Para forcar mesmo com outro dragao vivo:

```text
/dragaoend nascer force
/dragaoend spawn force
```

### Matar/remover dragoes

Remove apenas dragoes criados pelo plugin:

```text
/dragaoend matar
```

Remove qualquer Ender Dragon vivo no mundo configurado:

```text
/dragaoend matar todos
```

### Boss bar

Ativar:

```text
/dragaoend bossbar on
```

Desativar:

```text
/dragaoend bossbar off
```

A boss bar mostra o tempo no titulo, mas o preenchimento da barra mostra a vida atual do dragao.

### NPC campeao

Salvar o local do NPC:

```text
/dragaoend npc set
```

Use esse comando parado no local onde o armor stand deve ficar. O plugin salva mundo, coordenadas e a direcao que voce estava olhando.

Depois disso, sempre que um dragao for morto por jogadores, o NPC desse local muda para o matador do dragao:

- cabeca do player
- nick acima do armor stand
- peitoral, calca e bota de netherite
- espada de netherite na mao
- pose pronta de campeao

Ver status:

```text
/dragaoend npc status
```

Remover o armor stand atual, mantendo o local salvo para voltar no proximo dragao morto:

```text
/dragaoend npc remove
```

Ativar/desativar:

```text
/dragaoend npc on
/dragaoend npc off
```

Tambem da para salvar o local direto com:

```text
/dragaoend setnpc
```

### Configurar recompensa do top 1

O OP deve segurar o item na mao principal e usar:

```text
/dragaoend setreward top1
```

O plugin salva esse item na config e entrega uma copia para o jogador que ficar em top 1 de dano quando o dragao morrer por jogadores.

O item salvo preserva:

- quantidade
- nome customizado
- lore
- encantamentos
- outros dados salvos pelo Paper no `ItemStack`

Se o inventario do jogador estiver cheio, o restante cai no chao perto dele.

Ver recompensa configurada:

```text
/dragaoend recompensa status
```

Remover recompensa:

```text
/dragaoend clearreward top1
```

Tambem funciona:

```text
/dragaoend recompensa clear top1
```

### Resetar contador

```text
/dragaoend reset
```

Reinicia o contador do proximo spawn a partir do momento atual.

### Recarregar config

```text
/dragaoend reload
```

Recarrega o `config.yml` sem precisar reiniciar o servidor.

### Ativar/desativar agendamento

Ativar:

```text
/dragaoend enable
```

Desativar:

```text
/dragaoend disable
```

## Configuracoes importantes

Arquivo:

```text
plugins/EnderDragonSpawnerPlugin/config.yml
```

### Mundo e coordenadas

```yml
world: world_the_end

spawn:
  x: 0.0
  y: 80.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0
```

### Intervalo automatico

```yml
interval-hours: 3.0
```

`3.0` significa de 3h em 3h. Pelo comando tambem da para usar minutos, por exemplo `30m`.

### Tempo para matar

```yml
dragon:
  kill-time-minutes: 15.0
```

### Protecao de blocos

```yml
block-protection:
  enabled: true
  only-plugin-dragons: true
```

Com isso ligado, o dragao protegido nao quebra nenhum bloco.

### Boss bar

```yml
bossbar:
  enabled: true
  title: "&5Dragao do End &7- &e{time} &7para matar"
  progress-mode: HEALTH
  color: PURPLE
  style: SOLID
  show-to-all-online: false
```

`show-to-all-online: false` mostra a boss bar so para jogadores no mundo do dragao.

`show-to-all-online: true` mostra para todos online.

### Recompensa do top 1

Depois de usar `/dragaoend setreward top1`, a config fica preenchida em:

```yml
rewards:
  top1:
    enabled: true
    item:
      ==: org.bukkit.inventory.ItemStack
      type: DIAMOND
      amount: 1
    message: "&d{player} &7recebeu a recompensa de top 1: &f{item}&7 x{amount}."
```

Para itens simples, da para editar pela host. Para item com nome, lore ou encantamento, e mais seguro setar pelo comando segurando o item.

### NPC campeao

```yml
champion-npc:
  enabled: true
  update-on-defeat: true
  name: "&5Campeao do Dragao: &f{player}"
  gravity: false
  invulnerable: true
  world: world
  x: 0.0
  y: 80.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0
  last-player:
    uuid: ""
    name: ""
```

O jeito mais facil e usar `/dragaoend npc set`, porque ele ja salva a posicao e a direcao certinhas.

## Comportamento do dragao

- O dragao nasce no mundo e coordenadas configurados.
- O plugin nao usa o ritual vanilla de respawn.
- O dragao protegido nao quebra blocos.
- Se os jogadores matarem o dragao, aparece mensagem de vitoria com top 3 de dano.
- O top 1 de dano recebe a recompensa configurada.
- Se o NPC campeao estiver ativado, o armor stand troca para o matador do dragao.
- Se o tempo acabar, o dragao morre pela animacao vanilla, mas nao dropa XP/recompensa/ovo e nao aparece top dano.
- Depois que some por tempo ou morre, o proximo spawn segue o intervalo configurado.
