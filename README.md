# EnderDragon Spawner Plugin

Plugin Paper 1.21.11 para spawnar Ender Dragons no End em um intervalo configurável, sem usar o ritual vanilla de respawn e sem quebrar blocos.

## Novidades da versão 1.16.0

- Corrigida a acentuação dos textos em português.
- README, guia, changelog e config padrão foram revisados.
- Mensagens do plugin no chat/log agora aparecem com acentos.
- Não adiciona nenhuma dependência extra.

## Download

Baixe o plugin aqui:

[EnderDragon-Spawner-Plugin-1.16.0-paper-1.21.11.jar](https://github.com/eemppty/EnderDragon-Spawner-Plugin/releases/download/v1.16.0/EnderDragon-Spawner-Plugin-1.16.0-paper-1.21.11.jar)

Depois coloque o arquivo `.jar` na pasta `plugins/` do servidor.

Guia completo de comandos: veja `GUIA_COMANDOS.md`.

Histórico de updates: veja `CHANGELOG.md`.

## Instalar

1. Coloque `EnderDragon-Spawner-Plugin-1.16.0-paper-1.21.11.jar` na pasta `plugins/` do servidor Paper 1.21.11.
2. Reinicie o servidor.
3. Ajuste com os comandos abaixo ou edite `plugins/EnderDragonSpawnerPlugin/config.yml`.

## Comandos

### Comandos gerais

- `/dragaoend help` - mostra a ajuda.
- `/dragaoend status` - mostra mundo, coordenadas, intervalo e próximo spawn.
- `/dragaoend reload` - recarrega a config.

### Comandos de tempo e agendamento

- `/dragaoend setinterval <tempo>` - configura o intervalo. Exemplos: `10m`, `30m`, `90m`, `3`, `3h`, `5h`, `1.5h`.
- `/dragaoend setkilltime <tempo>` - configura quanto tempo os players têm para matar o dragão. Exemplos: `5m`, `15m`, `30m`, `1h`.
- `/dragaoend reset` - reinicia o contador a partir de agora.
- `/dragaoend enable` / `/dragaoend disable` - ativa ou desativa o agendamento.

### Comandos de spawn e local

- `/dragaoend setcoords <x> <y> <z> [mundo]` - configura as coordenadas do spawn.
- `/dragaoend sethere` - usa sua posição atual como spawn. Precisa estar no End.
- `/dragaoend nascer` - spawna manualmente se não houver dragão vivo.
- `/dragaoend nascer force` - spawna manualmente mesmo se já houver outro dragão.
- `/dragaoend spawn` - spawna manualmente se não houver dragão vivo.
- `/dragaoend spawn force` - spawna manualmente mesmo se já houver outro dragão.

### Comandos para matar ou remover dragões

- `/dragaoend matar` - remove os dragões criados pelo plugin no mundo configurado.
- `/dragaoend matar todos` - remove qualquer Ender Dragon vivo no mundo configurado.

### Comandos de boss bar

- `/dragaoend bossbar on` / `/dragaoend bossbar off` - ativa ou desativa a boss bar do cronômetro.

### Comandos de NPC campeão

- `/dragaoend npc set` - salva sua posição e direção como local fixo do NPC campeão.
- `/dragaoend npc status` - mostra o local e o jogador salvo no NPC campeão.
- `/dragaoend npc remove` - remove o armor stand atual, mantendo o local salvo para o próximo dragão morto.
- `/dragaoend npc on` / `/dragaoend npc off` - ativa ou desativa o NPC campeão.
- `/dragaoend setnpc` - atalho para salvar o local do NPC campeão.

### Comandos de recompensa

- `/dragaoend setreward top1` - segurando um item na mão, salva esse item como recompensa do top 1 de dano.
- `/dragaoend clearreward top1` - remove a recompensa do top 1.

Permissão: `enddragonsafe.admin` (padrão: OP).

## Como ele protege os blocos

O plugin não chama o respawn vanilla do Ender Dragon. Ele spawna a entidade diretamente na coordenada configurada. Além disso, quando `block-protection.enabled: true`, qualquer tentativa de quebrar blocos causada pelos dragões protegidos é cancelada. Isso vale para qualquer tipo de bloco, não só torres.

## Cronômetro e despawn

Por padrão, cada dragão nasce com 15 minutos para ser morto. O nome dele mostra o cronômetro restante. Se o tempo acaba, o plugin remove o dragão e o próximo nascimento continua seguindo o intervalo configurado em `/dragaoend setinterval`.

A boss bar também mostra o cronômetro no título, mas o preenchimento da barra representa a vida atual do dragão. Ela pode ser configurada em `plugins/EnderDragonSpawnerPlugin/config.yml`, na seção `bossbar`.

Quando o tempo chega no final, o plugin mata o dragão usando a animação vanilla de morte. Essa morte é marcada como expirada por tempo: não anuncia top dano, não entrega XP, não deixa drops, cancela ovo/portal do dragão quando a API dispara esses eventos, e mostra apenas a mensagem de desaparecimento por tempo.

Na versão 1.16.0, essa morte por tempo também corta o som de morte do Ender Dragon. Já a morte normal por players continua com som, mas em volume menor configurável.

## Mensagem de vitória

Quando o dragão morre, o plugin anuncia quem matou e o top 3 jogadores que mais deram dano. O ranking usa dano numérico, não porcentagem. A mensagem pode ser editada na config, em `messages.defeat`.

## Recompensa do top 1

Um OP pode segurar um item na mão principal e usar `/dragaoend setreward top1`. O plugin salva esse item na config, incluindo quantidade, nome, lore e encantamentos. Sempre que o dragão for morto por jogadores, o top 1 de dano recebe uma cópia dessa recompensa. Se o inventário estiver cheio, o restante cai perto do jogador.

## NPC campeão

Use `/dragaoend npc set` parado no bloco onde você quer deixar a estátua. O plugin salva o mundo, centraliza o armor stand no meio do bloco, e usa a direção do player. A partir daí, sempre que um dragão for morto por jogadores, o armor stand desse local troca para o matador do dragão: cabeça do player, apenas o nick acima, armadura full netherite sem capacete e espada de netherite na mão.
