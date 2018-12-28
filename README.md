<h1 align="center"> Auction House</h1>

<p align="center">
    <img src="https://travis-ci.com/herulume/SD.svg?token=aipGLrKNf4KH91HZ2mFw&branch=master" alt="Build Status">
</p>

## SD


## Base Structure

### Main
- auctionHouse : `AuctionHouse`
- socketServer :`SocketServer`

### AuctionHouse (mutable)
- stock     :`ThreadSafeMap<ServerType, Integer>`
- auctions  :`ThreadSafeMap<ServerType, Auction>`
- reserved  :`ThreadSafeMap<Integer, Droplet>`
- auctioned :`ThreadSafeMap<Integer, Droplet>`
- clients   :`ThreadSafeMap<String, Client>`

### Client (mutable)
- email    :`String`
- passowrd :`String`
- socket   :`Socket @Nullable` (mutable atribute)

### Droplet
- id          :`int`
- type        :`ServerType`
- clientEmail :`String`

### Auction
- id     :`int`
- type   :`ServerType`
- owner  :`String`
- amount :`int`

### Client-Connection (Worker Thread)
- auctionHouse :`AuctionHouse`
- Socket       :`Socket`
- clientEmail  :`String`

### ServerType
Enum with server types and their prices

## Extra points
Reutilize Workers (Client-Connection)
