# SD

## Base Structure

### Main
- auctionHouse : `AuctionHouse`
- clients :`Map<String, Client>`
- socketServer :`SocketServer`


### AuctionHouse (mutable)
- stock :`List<Item>`
- auctions :`Map<Integer, TopBid>`
- reserved :`Map<String, Droplet>`

### Client (mutable but not in thread)
- email :`String`
- passowrd :`String`
- socket :`Socket @Nullable`

### Item
- id: `int`
- type :`ServerType`
- price :`int`

### Droplet
- item :`Item`
- clientEmail :`String`

### TopBid
- item :`Item`
- owner :`String`
- amount :`int`

### Client-Connection (Worker Thread)
- auctionHouse :`AuctionHouse`
- Socket :`Socket`
- clientEmail :`String`

### ServerType
Enum with server types

## Extra points
Reutilize Workers (Client-Connection)
