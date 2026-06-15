# EnderDragon Spawner Plugin - Guia de Uso

Plugin para Paper 1.21.11 que spawna um Dragão do End automaticamente no End, com tempo para matar, boss bar, top dano, recompensa para top 1 e proteção para o dragão não quebrar blocos.

## Instalação

1. Coloque o arquivo `EnderDragon-Spawner-Plugin-1.16.0-paper-1.21.11.jar` na pasta `plugins/`.
2. Reinicie o servidor.
3. O plugin vai criar a config em:

```text
plugins/EnderDragonSpawnerPlugin/config.yml
```

4. Configure por comandos ou edite a config pela host.

Permissão principal:

```text
enddragonsafe.admin
```

Por padrão, OP já tem permissão.

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

Mostra a lista básica de comandos dentro do servidor.

### Status

```text
/dragaoend status
```

Mostra se o plugin está ativo, mundo/coordenada do spawn, intervalo, tempo para matar, próximo spawn, boss bar, proteção de blocos e recompensa do top 1.

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

Esse intervalo define de quanto em quanto tempo o dragão nasce. Se o dragão sumir por tempo, ele só volta no próximo ciclo configurado.

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

Se os jogadores não matarem o dragão nesse tempo, ele morre pela animação vanilla do Minecraft, não dropa XP, não dropa item, não gera ovo/portal pelo plugin, e aparece no chat apenas que ele desapareceu porque o tempo acabou.

### Configurar coordenadas do spawn

```text
/dragaoend setcoords <x> <y> <z> [mundo]
```

Exemplo:

```text
/dragaoend setcoords 0 80 0 world_the_end
```

O mundo precisa ser do tipo `THE_END`.

### Usar sua posição atual como spawn

```text
/dragaoend sethere
```

Use estando dentro do End. O plugin salva sua posição atual como local de nascimento do dragão.

### Spawnar o dragão manualmente

```text
/dragaoend nascer
```

Também funciona:

```text
/dragaoend spawn
```

Se já existir um Ender Dragon vivo no mundo configurado, o plugin pode impedir outro spawn dependendo da config.

Para forçar mesmo com outro dragão vivo:

```text
/dragaoend nascer force
/dragaoend spawn force
```

### Matar/remover dragões

Remove apenas dragões criados pelo plugin:

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

A boss bar mostra o tempo no título, mas o preenchimento da barra mostra a vida atual do dragão.

### NPC campeão

Salvar o local do NPC:

```text
/dragaoend npc set
```

Use esse comando parado no bloco onde o armor stand deve ficar. O plugin salva mundo, coloca o armor stand exatamente no centro do bloco, e usa a direção que você estava olhando.

Depois disso, sempre que um dragão for morto por jogadores, o NPC desse local muda para o matador do dragão:

- cabeça do player
- apenas o nick acima do armor stand
- peitoral, calça e bota de netherite
- espada de netherite na mão
- pose pronta de campeão

Ver status:

```text
/dragaoend npc status
```

Remover o armor stand atual, mantendo o local salvo para voltar no próximo dragão morto:

```text
/dragaoend npc remove
```

Ativar/desativar:

```text
/dragaoend npc on
/dragaoend npc off
```

Também dá para salvar o local direto com:

```text
/dragaoend setnpc
```

### Configurar recompensa do top 1

O OP deve segurar o item na mão principal e usar:

```text
/dragaoend setreward top1
```

O plugin salva esse item na config e entrega uma cópia para o jogador que ficar em top 1 de dano quando o dragão morrer por jogadores.

O item salvo preserva:

- quantidade
- nome customizado
- lore
- encantamentos
- outros dados salvos pelo Paper no `ItemStack`

Se o inventário do jogador estiver cheio, o restante cai no chão perto dele.

Ver recompensa configurada:

```text
/dragaoend recompensa status
```

Remover recompensa:

```text
/dragaoend clearreward top1
```

Também funciona:

```text
/dragaoend recompensa clear top1
```

### Resetar contador

```text
/dragaoend reset
```

Reinicia o contador do próximo spawn a partir do momento atual.

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

## Configurações importantes

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

### Intervalo automático

```yml
interval-hours: 3.0
```

`3.0` significa de 3h em 3h. Pelo comando também dá para usar minutos, por exemplo `30m`.

### Tempo para matar

```yml
dragon:
  kill-time-minutes: 15.0
```

### Som da morte do dragão

```yml
sounds:
  dragon-death:
    stop-on-timeout: true
    timeout-stop-ticks: 80
    reduce-normal-kill: true
    normal-kill-volume: 0.35
    normal-kill-pitch: 1.0
    apply-to-all-online: false
    radius-blocks: 512.0
```

`stop-on-timeout: true` corta o som de morte quando o dragão desaparece porque o tempo acabou.

`normal-kill-volume: 0.35` deixa a morte normal mais baixa quando os jogadores matam o dragão. Para aumentar ou diminuir, edite esse valor pela host e use `/dragaoend reload`.

`apply-to-all-online: false` aplica o ajuste apenas para jogadores no mesmo mundo e dentro do raio configurado.

### Proteção de blocos

```yml
block-protection:
  enabled: true
  only-plugin-dragons: true
```

Com isso ligado, o dragão protegido não quebra nenhum bloco.

### Boss bar

```yml
bossbar:
  enabled: true
  title: "&5Dragão do End &7- &e{time} &7para matar"
  progress-mode: HEALTH
  color: PURPLE
  style: SOLID
  show-to-all-online: false
```

`show-to-all-online: false` mostra a boss bar só para jogadores no mundo do dragão.

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

Para itens simples, dá para editar pela host. Para item com nome, lore ou encantamento, é mais seguro setar pelo comando segurando o item.

### NPC campeão

```yml
champion-npc:
  enabled: true
  update-on-defeat: true
  name: "{player}"
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

O jeito mais fácil é usar `/dragaoend npc set`, porque ele já centraliza o armor stand no bloco e salva a direção certinha.

## Comportamento do dragão

- O dragão nasce no mundo e coordenadas configurados.
- O plugin não usa o ritual vanilla de respawn.
- O dragão protegido não quebra blocos.
- Se os jogadores matarem o dragão, aparece mensagem de vitória com top 3 de dano.
- O top 1 de dano recebe a recompensa configurada.
- Se o NPC campeão estiver ativado, o armor stand troca para o matador do dragão.
- Se o tempo acabar, o dragão morre pela animação vanilla, mas não dropa XP/recompensa/ovo e não aparece top dano.
- Depois que some por tempo ou morre, o próximo spawn segue o intervalo configurado.
