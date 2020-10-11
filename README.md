# Spring WebFlux

### Webflux roda sob em Reactor
- Baseados em eventos
- Tudo e data streams
- Processamento asynchronous
- Requisições não bloqueantes
- Backpressure, ou seja, tratativa de pressão, o inscrito no evento comunica ao publicar a sua capacidade de processar.
- Programação funcional.
- Publicador cold (publicadores preguisoso), ou seja, somente emitirá eventos quando alguem se inscrever nele.
- Publicador hot, publicará os eventos mesmo que não há alguem inscrito.


#### Interfaces
- Publisher: publica os eventos
- Subscriber: recebe os eventos
- Subscription: é a assinatura, é a ação no momento do subscriber se "inscrever" no publisher.
- Processor: é ao mesmo tempo um subscriber e um publisher.
