# SD

## Base Structure

### Main
- auctionHouse : `AuctionHouse`
- socketServer :`SocketServer`

### AuctionHouse (mutable)
- stock :`List<Item>`
- auctions :`Map<Integer, TopBid>`
- reserved :`Map<String, Droplet>`
- clients :`Map<String, Client>`

### Client (mutable)
- email :`String`
- passowrd :`String`
- socket :`Socket @Nullable` (mutable atribute)

### Item
- id: `int`
- type :`ServerType`

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
Enum with server types and their prices

## Extra points
Reutilize Workers (Client-Connection)
