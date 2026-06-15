# EnderDragon Spawner Plugin

Plugin Paper 1.21.11 para spawnar Ender Dragons no End em um intervalo configuravel, sem usar o ritual vanilla de respawn e sem quebrar blocos.

## Novidades da versao 1.14.0

- `/dragaoend setnpc` e `/dragaoend npc set` agora colocam o armor stand exatamente no centro do bloco.
- O NPC campeao continua olhando para a mesma direcao do player que usou o comando.
- O nome acima do NPC agora mostra apenas o nick do jogador que matou o dragao.
- A config padrao do NPC agora usa `champion-npc.name: "{player}"`.

## Download

Baixe o plugin aqui:

[EnderDragon-Spawner-Plugin-1.14.0-paper-1.21.11.jar](https://github.com/eemppty/EnderDragon-Spawner-Plugin/releases/download/v1.14.0/EnderDragon-Spawner-Plugin-1.14.0-paper-1.21.11.jar)

Depois coloque o arquivo `.jar` na pasta `plugins/` do servidor.

Guia completo de comandos: veja `GUIA_COMANDOS.md`.

Historico de updates: veja `CHANGELOG.md`.

## Instalar

1. Coloque `EnderDragon-Spawner-Plugin-1.14.0-paper-1.21.11.jar` na pasta `plugins/` do servidor Paper 1.21.11.
2. Reinicie o servidor.
3. Ajuste com os comandos abaixo ou edite `plugins/EnderDragonSpawnerPlugin/config.yml`.

## Comandos

- `/dragaoend help` - mostra a ajuda.
- `/dragaoend status` - mostra mundo, coordenadas, intervalo e proximo spawn.
- `/dragaoend setinterval <tempo>` - configura o intervalo. Exemplos: `10m`, `30m`, `90m`, `3`, `3h`, `5h`, `1.5h`.
- `/dragaoend setkilltime <tempo>` - configura quanto tempo os players tem para matar o dragao. Exemplos: `5m`, `15m`, `30m`, `1h`.
- `/dragaoend setcoords <x> <y> <z> [mundo]` - configura as coordenadas do spawn.
- `/dragaoend sethere` - usa sua posicao atual como spawn. Precisa estar no End.
- `/dragaoend nascer` - spawna manualmente se nao houver dragao vivo.
- `/dragaoend nascer force` - spawna manualmente mesmo se ja houver outro dragao.
- `/dragaoend spawn` - spawna manualmente se nao houver dragao vivo.
- `/dragaoend spawn force` - spawna manualmente mesmo se ja houver outro dragao.
- `/dragaoend matar` - remove os dragoes criados pelo plugin no mundo configurado.
- `/dragaoend matar todos` - remove qualquer Ender Dragon vivo no mundo configurado.
- `/dragaoend bossbar on` / `/dragaoend bossbar off` - ativa ou desativa a boss bar do cronometro.
- `/dragaoend npc set` - salva sua posicao e direcao como local fixo do NPC campeao.
- `/dragaoend npc status` - mostra o local e o jogador salvo no NPC campeao.
- `/dragaoend npc remove` - remove o armor stand atual, mantendo o local salvo para o proximo dragao morto.
- `/dragaoend npc on` / `/dragaoend npc off` - ativa ou desativa o NPC campeao.
- `/dragaoend setreward top1` - segurando um item na mao, salva esse item como recompensa do top 1 de dano.
- `/dragaoend clearreward top1` - remove a recompensa do top 1.
- `/dragaoend reset` - reinicia o contador a partir de agora.
- `/dragaoend reload` - recarrega a config.
- `/dragaoend enable` / `/dragaoend disable` - ativa ou desativa o agendamento.

Permissao: `enddragonsafe.admin` (padrao: OP).

## Como ele protege os blocos

O plugin nao chama o respawn vanilla do Ender Dragon. Ele spawna a entidade diretamente na coordenada configurada. Alem disso, quando `block-protection.enabled: true`, qualquer tentativa de quebrar blocos causada pelos dragoes protegidos e cancelada. Isso vale para qualquer tipo de bloco, nao so torres.

## Cronometro e despawn

Por padrao, cada dragao nasce com 15 minutos para ser morto. O nome dele mostra o cronometro restante. Se o tempo acaba, o plugin remove o dragao e o proximo nascimento continua seguindo o intervalo configurado em `/dragaoend setinterval`.

A boss bar tambem mostra o cronometro no titulo, mas o preenchimento da barra representa a vida atual do dragao. Ela pode ser configurada em `plugins/EnderDragonSpawnerPlugin/config.yml`, na secao `bossbar`.

Quando o tempo chega no final, o plugin mata o dragao usando a animacao vanilla de morte. Essa morte e marcada como expirada por tempo: nao anuncia top dano, nao entrega XP, nao deixa drops, cancela ovo/portal do dragao quando a API dispara esses eventos, e mostra apenas a mensagem de desaparecimento por tempo.

## Mensagem de vitoria

Quando o dragao morre, o plugin anuncia quem matou e o top 3 jogadores que mais deram dano. O ranking usa dano numerico, nao porcentagem. A mensagem pode ser editada na config, em `messages.defeat`.

## Recompensa do top 1

Um OP pode segurar um item na mao principal e usar `/dragaoend setreward top1`. O plugin salva esse item na config, incluindo quantidade, nome, lore e encantamentos. Sempre que o dragao for morto por jogadores, o top 1 de dano recebe uma copia dessa recompensa. Se o inventario estiver cheio, o restante cai perto do jogador.

## NPC campeao

Use `/dragaoend npc set` parado no bloco onde voce quer deixar a estatua. O plugin salva o mundo, centraliza o armor stand no meio do bloco, e usa a direcao do player. A partir dai, sempre que um dragao for morto por jogadores, o armor stand desse local troca para o matador do dragao: cabeca do player, apenas o nick acima, armadura full netherite sem capacete e espada de netherite na mao.
