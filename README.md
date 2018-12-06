# SD

## Base Structure

### Main
- auctionHouse : `AuctionHouse`
- clients :`List<Client>` // (email :String, password :String, socket :Socket)
- socketServer :`SocketServer`


### AuctionHouse
- stock :`HashMap<String, Item>` // (type :String, Price :int, Amount :int)
- auctions :`List<TopBid>`
- reserved :`List<Droplet>`

### Client
- email :`String`
- passowrd :`String`
- socket :`Socket @Nullable`

### Item
- type :`String`
- price :`int`
- amount :`int`

### Droplet
- type :`String`
- client :`String` // email

### TopBid
- type :`String`
- amount :`String`

### Client-Con (In Thread)
- auctionHouse :`AuctionHouse`
- Socket :`Socket`
- clientEmail :`String`


## Extra points
Reutilizar workers (Client-Con)
