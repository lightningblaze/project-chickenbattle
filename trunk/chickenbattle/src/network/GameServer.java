package network;

import java.io.IOException;
import java.util.HashMap;

import network.Packet.AddPlayer;
import network.Packet.Added;
import network.Packet.BlockUpdate;
import network.Packet.Bullet;
import network.Packet.Disconnected;
import network.Packet.Hit;
import network.Packet.Reject;
import network.Packet.Update;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class GameServer {
	Server server;
	Player[] gameState;
	Vector3[] bbCorners;
	HashMap<Connection,Integer> connectionIDs;
	Connection[] connections;
	Update toSend;
	BlockUpdate btoSend;
	Hit hittoSend;
	Vector3 point;
	Vector3 direction;
	int startx,starty,startz;
	int ids;
	boolean hit;

	public GameServer () throws IOException {
		server = new Server();
		gameState = new Player[10];
		connections = new Connection[10];
		bbCorners = new Vector3[8];
		for(int i=0; i < 8; i++)
			bbCorners[i] = new Vector3(0,0,0);
		point = new Vector3(0,0,0);
		direction = new Vector3(0,0,0);
		toSend = new Update();
		hittoSend = new Hit();
		connectionIDs = new HashMap<Connection,Integer>();
		server.start();
		Packet.register(server);

		server.bind(54555, 54778);   
		startx = 0;
		starty = 0;
		startz = 0;

		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof AddPlayer){
					AddPlayer received = (AddPlayer)object;
					System.out.println("Adding player: " + received.name);
					if(fixId()){
						Added reply = new Added();					
						reply.id = ids;
						System.out.println(received.name + "gets id: " + ids);
						connection.sendTCP(reply);
						AddPlayer oldPlayers = new AddPlayer();
						for(int i=0; i <gameState.length; i++){
							if(gameState[i] != null){
								oldPlayers.id = i;
								oldPlayers.name = gameState[i].name;
								oldPlayers.startx = gameState[i].posX;
								oldPlayers.starty = gameState[i].posY;
								oldPlayers.startz = gameState[i].posZ;
								connection.sendTCP(oldPlayers);
							}
						}
						gameState[ids] = new Player(received.name);
						connections[ids] = connection; 
						connectionIDs.put(connection, ids);

						AddPlayer newPlayer = new AddPlayer();
						newPlayer.id = ids;
						newPlayer.name = received.name;
						newPlayer.startx = startx;
						newPlayer.starty = starty;
						newPlayer.startz = startz;
						server.sendToAllTCP(newPlayer);
					}
					else{
						connection.sendTCP(new Reject());
					}
				}

				else if (object instanceof Update) {
					Update received = (Update)object;
					toSend.id = received.id;
					toSend.x = received.x;
					toSend.y = received.y;	
					toSend.z = received.z;	
					toSend.kills = gameState[received.id].kills;
					toSend.deaths = gameState[received.id].deaths;
					toSend.hp = gameState[received.id].hp;

					gameState[received.id].posX = received.x;
					gameState[received.id].posY = received.y;
					gameState[received.id].posZ = received.z;
					
					bbCorners[0].set(received.x1, received.y1, received.z1);
					bbCorners[1].set(received.x2, received.y2, received.z2);
					bbCorners[2].set(received.x3, received.y3, received.z3);
					bbCorners[3].set(received.x4, received.y4, received.z4);
					bbCorners[4].set(received.x5, received.y5, received.z5);
					bbCorners[5].set(received.x6, received.y6, received.z6);
					bbCorners[6].set(received.x7, received.y7, received.z7);
					bbCorners[7].set(received.x8, received.y8, received.z8);		

					toSend.x1 = received.x1;
					toSend.y1 = received.y1;
					toSend.z1 = received.z1;

					toSend.x2 = received.x2;
					toSend.y2 = received.y2;
					toSend.z2 = received.z2;

					toSend.x3 = received.x3;
					toSend.y3 = received.y3;
					toSend.z3 = received.z3;

					toSend.x4 = received.x4;
					toSend.y4 = received.y4;
					toSend.z4 = received.z4;

					toSend.x5 = received.x5;
					toSend.y5 = received.y5;
					toSend.z5 = received.z5;

					toSend.x6 = received.x6;
					toSend.y6 = received.y6;
					toSend.z6 = received.z6;

					toSend.x7 = received.x7;
					toSend.y7 = received.y7;
					toSend.z7 = received.z7;

					toSend.x8 = received.x8;
					toSend.y8 = received.y8;
					toSend.z8 = received.z8;
					

					gameState[received.id].setBox(bbCorners);
					server.sendToAllTCP(toSend);
				}
				else if (object instanceof BlockUpdate){
					BlockUpdate received = (BlockUpdate)object;
					btoSend = received;
					server.sendToAllTCP(btoSend);		
				}
				else if(object instanceof Bullet){
					hit = false;
					float range = 0;
					Bullet b = (Bullet)object;
					direction.set(b.dx, b.dy, b.dz);
					point.set(b.ox,b.oy,b.oz);
					while (!hit && range < 200) {
						range += direction.len();
						point.add(direction);
						for(int i=0; i < gameState.length; i++){				
							if(gameState[i] != null){
								if(gameState[i].box.contains(point)){
									hittoSend.id = i;
									gameState[i].hp =gameState[i].hp-1;
									if(gameState[i].hp == 0){
										System.out.println(b.id + " killed " + i);
										gameState[b.id].kills += 1;
										System.out.println(b.id + " now haskilled " + gameState[b.id].kills );
										gameState[i].deaths += 1;
										gameState[i].hp = 10;
									}
									hit = true;
									server.sendToAllTCP(hittoSend);
								}
							}
						}
					}
				}
			}

			public void disconnected (Connection c) {
				if(connectionIDs.get(c)!= null){
					Disconnected dc = new Disconnected();	
					dc.id = connectionIDs.get(c);
					connectionIDs.remove(c);
					gameState[dc.id] = null;
					connections[dc.id] = null;
					server.sendToAllTCP(dc);
				}
			}
		});
	}

	public boolean fixId(){
		for(int i=0; i < gameState.length; i++){
			if(gameState[i] == null){
				ids = i;
				return true;
			}		 
		}
		return false;
	}
	public static void main (String[] args) throws IOException {
		new GameServer();
		System.out.println("rnning");
	}

}

