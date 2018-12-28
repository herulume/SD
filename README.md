<h1 align="center"> Auction House</h1>

<p align="center">
    <img src="https://travis-ci.com/herulume/SD.svg?token=aipGLrKNf4KH91HZ2mFw&branch=master" alt="Build Status">
</p>

# SD


## Base Structure

### Main
- Telnet like client : `Client`
- Server :`Server`

### Server
- Server : `Server`
- Auction House (mut) :`AuctionHouse` 
- Middleware to manage sessions :`Session`
- Parser / Command interpreter : `Parser`

### Parts of server
- Bid (USer / amount) : `Bid`
- Server to rent : `Droplet`
- Auction (mut)  : `Auction`
- Catalog of servers : `ServerType`
- Clients / Users : `User`

### Client
- Telnet like client : `Client`

## Extra points
Reutilize Workers (Client-Connection)
