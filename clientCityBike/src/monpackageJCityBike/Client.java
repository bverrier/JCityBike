package monpackageJCityBike;

import com.sun.javacard.apduio.CadT1Client;
import com.sun.javacard.apduio.CadTransportException;
import card.MonApplet;
import com.sun.javacard.apduio.Apdu;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.time.LocalTime;
import java.util.Scanner;
import java.util.Arrays;


public class Client {

	public static void main(String[] args)throws IOException, CadTransportException {
		CadT1Client cad;
		Socket sckCarte;
		try {
			sckCarte = new Socket("localhost", 9025);
			sckCarte.setTcpNoDelay(true);
			BufferedInputStream input = new BufferedInputStream(sckCarte.getInputStream());
			BufferedOutputStream output = new BufferedOutputStream(sckCarte.getOutputStream());
			cad = new CadT1Client(input, output);
			cad.powerUp();
		}
		catch (Exception e) {
			System.out.println("Erreur : impossible de se connecter a la Javacard");
			return;
		}
		/* Sélection de l'applet */
		Apdu apdu = new Apdu();
		apdu.command[Apdu.CLA] = 0x00;
		apdu.command[Apdu.INS] = (byte) 0xA4;
		apdu.command[Apdu.P1] = 0x04;
		apdu.command[Apdu.P2] = 0x00;
		byte[] appletAID = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x00, 0x00 };
		apdu.setDataIn(appletAID);
		cad.exchangeApdu(apdu);
		if (apdu.getStatus() != 0x9000) {
			System.out.println("Erreur lors de la sélection de l'applet");
			System.exit(1);
		}
		long EmpruntVeloTimestamp = 0;
		/* Menu principal */
		boolean fin = false;
		while (!fin) {
			System.out.println();
			System.out.println("Application cliente Javacard");
			System.out.println("----------------------------");
			System.out.println();
			System.out.println("1 - Interroger le compteur");
			System.out.println("2 - Emprunter un vélo");
			System.out.println("3 - Rendre un vélo");
			System.out.println("4 - Recharger abonnement");
			System.out.println("5 - Recharger unité");
			System.out.println("6 - Enlever suspension");
			System.out.println("7 - Débloquer carte");
			System.out.println("8 - Consulter l'historique");
			System.out.println("9 - Quitter");
			System.out.println("10 - Suspendre");
			System.out.println();
			Scanner scannerMenu = new Scanner(System.in);
			System.out.print("Votre choix ? ");
			String choixUserInput = scannerMenu.nextLine();
			int choixuser = -1;
			try {
			    choixuser = Integer.parseInt(choixUserInput);
			} catch (NumberFormatException e) {
			    System.out.println("Entrée invalide. Veuillez saisir un numéro.");
			    continue;
			}
			while (!(choixuser >= 1 && choixuser <= 12)) {
				choixuser = System.in.read();
			}
			apdu = new Apdu();
			apdu.command[Apdu.CLA] = MonApplet.CLA_MONAPPLET;
			apdu.command[Apdu.P1] = 0x00;
			apdu.command[Apdu.P2] = 0x00;
			switch (choixuser) {
				case 1:
					apdu.command[Apdu.INS] = MonApplet.INS_INTERROGER_BALANCE;
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() == 0x6a83) {
						System.out.println("Impossible la carte est bloquée");
					} else if (apdu.getStatus() == 0x6a84) {
						System.out.println("Impossible la carte est suspendue");
					}else {
						short s =  (short) (apdu.dataOut[0] << 8 | apdu.dataOut[1] & 0xFF);
						System.out.println("\nSolde : " + s);
						long mois = 0;
					    for (int i = 0; i < 8; i++) {
					    	mois = (mois << 8) | (apdu.dataOut[2 + i] & 0xFF);
					    }
					    long annee = 0;
					    for (int i = 0; i < 8; i++) {
					    	annee = (annee << 8) | (apdu.dataOut[10 + i] & 0xFF);
					    }
					    boolean abonnementValide = isCurrentMonthAndYearValid(mois, annee);
					    if(abonnementValide) {
                        	System.out.println("\nAbonnement actif");
                    	} else {
                    		System.out.println("\nAbonnement inactif");
                    	}
					}
					break;
				case 2:
					apdu.command[Apdu.INS] = MonApplet.INS_ETAT_CARTE;
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() == 0x6a83) {
						System.out.println("Impossible la carte est bloquée");
					} else if (apdu.getStatus() == 0x6a84) {
						System.out.println("Impossible la carte est suspendue");
					} else {
						byte[] data = apdu.dataOut;
						if (data[0] != 0) {
							System.out.println("Carte suspendue");
							break;
						}
					}
					apdu.command[Apdu.INS] = MonApplet.INS_ETAT_ABONNEMENT;
				    cad.exchangeApdu(apdu);
				    boolean abonnementValide = false;
				    if (apdu.getStatus() != 0x9000) {
                        System.out.println("Erreur : status word différent de 0x9000");
                    } else {
                        byte[] data = apdu.dataOut;
                        long mois = ((data[0] & 0xFFL) << 24) | 
                                ((data[1] & 0xFFL) << 16) | 
                                ((data[2] & 0xFFL) << 8)  | 
                                (data[3] & 0xFFL);
		               long annee = ((data[4] & 0xFFL) << 24) | 
		                              ((data[5] & 0xFFL) << 16) | 
		                              ((data[6] & 0xFFL) << 8)  | 
		                              (data[7] & 0xFFL);
		                abonnementValide = isCurrentMonthAndYearValid(mois, annee);
					    if(abonnementValide) {
					    	System.out.println("\nAbonnement actif\n");
	                   	} else {
	                   		System.out.println("\nAbonnement inactif\n");
	                   	}
                    }
				    
				    int nbElecEmpruntes = 0;
					apdu.command[Apdu.INS] = MonApplet.INS_EMPRUNTER_VELO;
					/*cad.exchangeApdu(apdu);
					if (apdu.getStatus() != 0x9000) {
						System.out.println("Erreur!!!! : status word différent de 0x9000  : "
								+ Integer.toHexString(apdu.getStatus()));
					} else {
						byte[] dataOut = apdu.getDataOut();
						if (dataOut.length < 10) {
							//System.out.println("Impossible aucun vélo emprunté");
						} else {
							short typeVelo = (short) dataOut[0];
							short idVelo = (short) (dataOut[1] << 8 | dataOut[2] & 0xFF);
							short cout = (short) (dataOut[3] << 8 | dataOut[4] & 0xFF);
							System.out.println("Vélo " + idVelo + " emprunté avec succès");
							EmpruntVeloTimestamp = ((long) dataOut[5] << 56 | (long) dataOut[6] << 48
									| (long) dataOut[7] << 40 | (long) dataOut[8] << 32 | (long) dataOut[9] << 24
									| (long) dataOut[10] << 16 | (long) dataOut[11] << 8 | (long) dataOut[12] & 0xFF);
						}
					}*/
					short[][] velos = {
				            {1, 101}, // Vélo classique, ID = 101
				            {1, 102}, // Vélo classique, ID = 102
				            {2, 201}, // Vélo électrique, ID = 201
				            {2, 202}  // Vélo électrique, ID = 202
				        };

				        // Afficher les options de vélos
				        System.out.println("Sélectionnez un vélo parmi les options disponibles :");
				        for (int i = 0; i < velos.length; i++) {
				            String typeVelo = (velos[i][0] == 1) ? "Classique" : "Électrique";
				            System.out.printf("%d. Vélo %s (ID: %d)\n", i + 1, typeVelo, velos[i][1]);
				        }

				        // Demander à l'utilisateur de choisir un vélo
				        Scanner scanner = new Scanner(System.in);
				        int choix = -1;
				        while (choix < 1 || choix > velos.length) {
				            System.out.print("Entrez le numéro correspondant à votre choix : ");
				            choix = scanner.nextInt();
				        }

				        // Récupérer les informations sur le vélo choisi
				        short typeVeloChoisi = velos[choix - 1][0];
				        short idVeloChoisi = velos[choix - 1][1];

				        // Liste de dates possibles avec leurs timestamps
				        Calendar[] datesPossibles = new Calendar[] {
				            new GregorianCalendar(2025, Calendar.JANUARY, 1, 10, 0, 0),
				            new GregorianCalendar(2025, Calendar.JANUARY, 2, 14, 0, 0),
				            new GregorianCalendar(2025, Calendar.JANUARY, 5, 9, 30, 0),
				            new GregorianCalendar(2025, Calendar.FEBRUARY, 10, 16, 0, 0)
				        };
				        
				        // Afficher les dates possibles
				        System.out.println("\nSélectionnez une date parmi les options suivantes :");
				        for (int i = 0; i < datesPossibles.length; i++) {
				            System.out.println((i + 1) + ": " + datesPossibles[i].getTime());
				        }

				        // Demander à l'utilisateur de choisir une date
				        int choixDate = -1;
				        while (choixDate < 1 || choixDate > datesPossibles.length) {
				            System.out.print("Entrez le numéro correspondant à votre choix : ");
				            choixDate = scanner.nextInt();
				        }

				        // Récupérer le timestamp de la date choisie (en millisecondes)
				        long timestamp = datesPossibles[choixDate - 1].getTimeInMillis();
				        //System.out.println("\nDate choisi (en millisecondes) : " + timestamp);
				        
				        // Calcul du coût de l'emprunt
				        short cout = 0;
				        if (typeVeloChoisi == 1) { // Vélo classique
				            if (abonnementValide == true) {
				                cout = 0; // Inclus dans l'abonnement
				            } else {
				                cout = 1; // 1 unité pour un vélo classique hors abonnement
				            }
				        } else if (typeVeloChoisi == 2) { // Vélo électrique
				            if (abonnementValide && nbElecEmpruntes < 5) {
				                cout = 0; // Inclus dans l'abonnement dans la limite des 5 premiers vélos
				            } else if (abonnementValide) {
				                cout = 2; // 2 unités pour un vélo électrique au-delà des 5 premiers
				            } else {
				                cout = 4; // 4 unités pour un vélo électrique hors abonnement
				            }
				        }
				     
				        // Simuler l'envoi d'une commande APDU avec le timestamp et les autres informations
				        byte[] dataE = new byte[13];  // 8 octets pour le timestamp + 4 autres pour le vélo
				        // Convertir le timestamp en octets (8 octets pour un long)
				        for (int i = 0; i < 8; i++) {
				            dataE[i] = (byte) (timestamp >> (56 - i * 8)); // Décalage de 8 bits par 8 bits
				        }
				        
				        System.out.println("Coût du vélo : " + cout);
				        // Ajouter les informations sur le vélo choisi (type, ID, coût)
				        dataE[8] = (byte) typeVeloChoisi;  // Type de vélo (1 ou 2)
				        dataE[9] = (byte) (idVeloChoisi >> 8);  // ID vélo (high byte)
				        dataE[10] = (byte) (idVeloChoisi & 0xFF);     // ID vélo (low byte)
				        dataE[11] = (byte) (cout>>8);  // Cout vélo (high byte)
                        dataE[12] = (byte) (cout & 0xFF);  // Cout vélo (low byte)
					
				    apdu.setDataIn(dataE);
				    cad.exchangeApdu(apdu);

				    if (apdu.getStatus() == 0x6A87) {
				    	System.out.println("\nSolde insuffisant pour emprunter le vélo " + idVeloChoisi);
				    } else if(apdu.getStatus() != 0x9000) {	
			    		System.out.println("Erreur! : status word différent de 0x9000  : "+ Integer.toHexString(apdu.getStatus()));
			    		
				    }else {
				    	System.out.println("Vélo " + idVeloChoisi + " emprunté avec succès à " + timestamp);
				    	EmpruntVeloTimestamp = timestamp;
				    }
				    break;
				case 3:
				    // Commande pour demander la restitution du vélo
				    apdu.command[Apdu.INS] = MonApplet.INS_RENDRE_VELO;
				    cad.exchangeApdu(apdu);
				    
				    // Vérifier si la commande a réussi
				    if (apdu.getStatus() == 0x6a83) {
						System.out.println("Impossible la carte est bloquée");
						break;
					} else if (apdu.getStatus() == 0x6a84) {
						System.out.println("Impossible la carte est suspendue");
						break;
					}
				    
				   //afficher status
				    byte[] dataOut = apdu.getDataOut();
					if (dataOut.length < 10) {
						System.out.println("\nImpossible aucun vélo emprunté");
						break;
					}
					
				    // Récupérer l'ID du vélo restitué
				    short idVeloRestitue = (short) (dataOut[0] << 8 | dataOut[1] & 0xFF);
				    
				    // Récupérer le timestamp de la restitution
				    long timestampRestitution = (dataOut[2] << 56 | dataOut[3] << 48
				            | dataOut[4] << 40 | dataOut[5] << 32 | dataOut[6] << 24
				            | dataOut[7] << 16 | dataOut[8] << 8 | dataOut[9] & 0xFF);
				    
				    // Afficher les informations sur le vélo restitué
				    System.out.println("Vélo " + idVeloRestitue + " restitué à " + timestampRestitution);
				    
				    // Choisir l'heure de restitution possible
				    System.out.println("Sélectionnez l'heure de restitution parmi les options suivantes :");
				    System.out.println("1: 1 heure après l'emprunt");
				    System.out.println("2: Le lendemain à 1h du matin");

				    // Demander à l'utilisateur de choisir l'option
				    scanner = new Scanner(System.in);
				    int choixRestitution = scanner.nextInt();
				    long timestampRestitutionChoisie = 0;

				    if (choixRestitution == 1) {
				        long timestampEmprunt = EmpruntVeloTimestamp;
				        // Ajouter 1 heure à l'emprunt pour la restitution
				        timestampRestitutionChoisie = timestampEmprunt + 3600000;
				    } else if (choixRestitution == 2) {
				        long timestampEmprunt = EmpruntVeloTimestamp;
				        // Calculer le timestamp du lendemain à 1h du matin
				        timestampRestitutionChoisie = createNextDayMidnightTimestamp(timestampEmprunt);
				    }

				    // Vérifier si la restitution choisie est après minuit
				    long timestampMinuitEmprunt = createMidnightTimestampFromEmprunt(EmpruntVeloTimestamp);
				    if (timestampRestitutionChoisie > timestampMinuitEmprunt) {
				        System.out.println("Restitution après minuit, carte suspendu.");
				        apdu.command[Apdu.INS] = MonApplet.INS_SUSPEND_TOI;
				        cad.exchangeApdu(apdu);
				    } else {
				        System.out.println("Restitution avant minuit, traitement normal.");
				    }
				    break;

			case 4:
				apdu.command[Apdu.INS] = MonApplet.INS_RECHARGER_ABONNEMENT_BALANCE;
				
				// Saisir le PIN (4 chiffres)
				Scanner scannerAbonnement = new Scanner(System.in);

				// Vider le buffer si nécessaire
				if (scannerAbonnement.hasNextLine()) {
				    scannerAbonnement.nextLine();
				}
				
				// Saisir le PIN (4 chiffres)
				System.out.print("Entrez votre PIN (4 chiffres) : ");
				String pinInputAbonnement = scannerAbonnement.nextLine();
				
				// Vérifier que le PIN contient bien 4 chiffres
				if (pinInputAbonnement.length() != 4) {
				    System.out.println("Erreur : le PIN doit contenir exactement 4 chiffres !");
				    break;
				}
				
				// Convertir le PIN en tableau de bytes
				byte[] pinData = new byte[4];
				for (int i = 0; i < 4; i++) {
				    pinData[i] = (byte) (pinInputAbonnement.charAt(i) - '0'); // Convertir chaque caractère en byte
				}
				
				// Récupérer la date actuelle
				long dateMillis = System.currentTimeMillis(); // Obtient le temps en millisecondes depuis l'époque
		
				// Convertir le temps en millisecondes en un tableau de 8 octets (longueur 8 pour le long)
				byte[] dateBytes = new byte[8];
				dateBytes[0] = (byte) (dateMillis >> 56);  // Premier octet (partie la plus significative)
				dateBytes[1] = (byte) (dateMillis >> 48);
				dateBytes[2] = (byte) (dateMillis >> 40);
				dateBytes[3] = (byte) (dateMillis >> 32);
				dateBytes[4] = (byte) (dateMillis >> 24);
				dateBytes[5] = (byte) (dateMillis >> 16);
				dateBytes[6] = (byte) (dateMillis >> 8);
				dateBytes[7] = (byte) (dateMillis);  // Dernier octet (partie la moins significative)
				
				// Créer le tableau de données à envoyer (4 octets pour le PIN et 8 pour la date)
				byte[] dataAbonnement = new byte[12];  // 4 octets pour le PIN, 8 octets pour la date
				System.arraycopy(pinData, 0, dataAbonnement, 0, 4);  // Copier le PIN
				System.arraycopy(dateBytes, 0, dataAbonnement, 4, 8);  // Ajouter la date (en millisecondes)
				
				// Envoyer les données à l'applet
				apdu.setDataIn(dataAbonnement);
				cad.exchangeApdu(apdu);
				int statusCode = apdu.getStatus();
				
				if (apdu.getStatus() == 0x6a83) {
					System.out.println("\nImpossible la carte est bloquée");
				} else if (apdu.getStatus() == 0x6a84) {
					System.out.println("\nImpossible la carte est suspendue");
				} else {
				    System.out.println("\nAbonnement rechargé avec succès !");
				}
				break;
				
			case 5:
				apdu.command[Apdu.INS] = MonApplet.INS_RECHARGER_UNITE_BALANCE;
				byte[] dataUnit = new byte[5]; // 4 pour le PIN, 1 pour le montant

			    // Saisir le PIN (4 chiffres)
			   // Scanner pour capturer les entrées utilisateur
			    scanner = new Scanner(System.in);
			
			    // Vider le buffer si nécessaire
			    if (scanner.hasNextLine()) {
			        scanner.nextLine();
			    }
			
			    // Saisir le PIN (4 chiffres)
			    System.out.print("Entrez votre PIN (4 chiffres) : ");
			    String pinInput = scanner.nextLine();
			
			    for (int i = 0; i < 4; i++) {
			        dataUnit[i] = (byte) (pinInput.charAt(i) - '0'); // Convertir chaque chiffre en byte
			    }
			
			    // Saisir le montant à recharger
			    System.out.print("Entrez le montant à recharger (1 à 62) : ");
			    int montant = scanner.nextInt();
			
			    dataUnit[4] = (byte) montant; // Ajouter le montant à la fin des données
			
			    apdu.setDataIn(dataUnit);
			    cad.exchangeApdu(apdu);
			
			    // Vérifier le status word
			    //System.out.println("Status code: " + Integer.toHexString(apdu.getStatus() & 0xFFFF));
			    if (apdu.getStatus() == 0x6A8B) {
					System.out.println("Impossible état de la carte non correcte");
			    } else {
			        System.out.println("Recharge de "+montant +" crédit(s) effectuée avec succès !");
			    }
			    break;
			case 6:
				apdu.command[Apdu.INS] = MonApplet.INS_ENLEVER_SUSPENSION_CARTE;
				byte[] codePUK = new byte[5];

			    while(apdu.getStatus() != 0x6985 && apdu.getStatus() != 0x9000) {
			    	scanner = new Scanner(System.in);
			
				    // Vider le buffer si nécessaire
				    if (scanner.hasNextLine()) {
				        scanner.nextLine();
				    }
				
				    // Saisir le PIN (4 chiffres)
				    System.out.print("Entrez votre PUK (5 chiffres) : ");
				    String pukInput = scanner.nextLine();
				    // Vérifier que le PIN contient bien 5 chiffres
					if (pukInput.length() != 5) {
					    System.out.println("Erreur : le PIN doit contenir exactement 5 chiffres !");
					    break;
					}
				    for (int i = 0; i < 5; i++) {
				    	codePUK[i] = (byte) (pukInput.charAt(i) - '0'); // Convertir chaque chiffre en byte
				    }
				    
				    apdu.setDataIn(codePUK);
				    cad.exchangeApdu(apdu);
				    
				    // Vérifier le status word
				    if(apdu.getStatus() == 0x6A83) {
	                	System.out.println("Carte bloquée");
	                	break;
					} else if (apdu.getStatus() == 0x6A89) {
						System.out.println("La carte n'est pas suspendue");
						break;
				    }else if(apdu.getStatus() == 0x6A88) {
	                	System.out.println("Code PUK incorrect");
				    }else if (apdu.getStatus() == 0x9000) {
				        System.out.println("Suspension levée avec succès !");
				        break;
				    }
			    }
				break;
			case 7:
				apdu.command[Apdu.INS] = MonApplet.INS_DEBLOQUER_CARTE;
				codePUK = new byte[5];

			    while(apdu.getStatus() != 0x6985 && apdu.getStatus() != 0x9000) {
			    	scanner = new Scanner(System.in);
			
				    if (scanner.hasNextLine()) {
				        scanner.nextLine();
				    }
				
				    // Saisir le PIN (4 chiffres)
				    System.out.print("Entrez votre PUK (5 chiffres) : ");
				    String pukInput = scanner.nextLine();
				    // Vérifier que le PIN contient bien 5 chiffres
					if (pukInput.length() != 5) {
					    System.out.println("Erreur : le PIN doit contenir exactement 5 chiffres !");
					    break;
					}
				    for (int i = 0; i < 5; i++) {
				    	codePUK[i] = (byte) (pukInput.charAt(i) - '0'); // Convertir chaque chiffre en byte
				    }
				    apdu.setDataIn(codePUK);
				    cad.exchangeApdu(apdu);
				    
				    // Vérifier le status word
				    if (apdu.getStatus() == 0x6A8A) {
						System.out.println("La carte n'est pas bloqué");
						break;
				    }else if(apdu.getStatus() == 0x6A88) {
	                	System.out.println("Code PUK incorrect");
				    }else if (apdu.getStatus() == 0x9000) {
				        System.out.println("Bloquage levée avec succès !");
				        break;
				    }
			    }
				break;
			case 8:
				apdu.command[Apdu.INS] = MonApplet.INS_CONSULTER_HISTORIQUE;
				scanner = new Scanner(System.in);
				codePUK = new byte[5];
			
			    if (scanner.hasNextLine()) {
			        scanner.nextLine();
			    }
			
			    // Saisir le PIN (4 chiffres)
			    System.out.print("Entrez votre PUK (5 chiffres) : ");
			    String pukInput = scanner.nextLine();
			    // Vérifier que le PIN contient bien 5 chiffres
				if (pukInput.length() != 5) {
				    System.out.println("Erreur : le PIN doit contenir exactement 5 chiffres !");
				    break;
				}
			    for (int i = 0; i < 5; i++) {
			    	codePUK[i] = (byte) (pukInput.charAt(i) - '0'); // Convertir chaque chiffre en byte
			    }
			    // Envoyer les données à la carte
			    apdu.setDataIn(codePUK);
			   
				cad.exchangeApdu(apdu);
				if (apdu.getStatus() == 0x6a83) {
					System.out.println("\nImpossible la carte est bloquée");
				} else if (apdu.getStatus() == 0x6a84) {
					System.out.println("\nImpossible la carte est suspendue");
				} else if (apdu.getStatus() == 0x6A8D) { // Aucun log trouvé
		            System.out.println("\nAucun historique disponible.");
		        } else if (apdu.getStatus() == 0x9000) { // Succès
		            byte[] responseData = apdu.getDataOut();
		            System.out.println("\nHistorique des actions :");
		            
		            // Décoder les logs
		            int numberOfLogs = responseData.length / 8; // Calcul du nombre de logs
		            for (int i = 0; i < numberOfLogs; i+=3 ) {
		                long log = 0;
		                for (int j = 0; j < 8; j++) {
		                    log = (log << 8) | (responseData[i * 8 + j] & 0xFF); // Reconstituer le long à partir des octets
		                }
		                int value = (int) log;
		                //System.out.println("Log " + (i + 1) + ": " + log); 
		                switch (value) {
							case 0:
								System.out.println("Action : Emprunt d'un vélo");
								long res = nextLog(responseData, i+1);
								if (res == (long)0) {
									System.out.println("Erreur emprunt type vélo");
								} else {
									System.out.println("ID du vélo : "+res);
									System.out.println("Cout : "+nextLog(responseData, i+2));
								}
								break;
	                        case 1:
	                        	System.out.println("Action : Restitution d'un vélo");
	                        	break;
	                        case 2:
			                    System.out.println("Action : Interroger la balance");
			              		System.out.println("Valeur : "+nextLog(responseData, i+1));
			              		//System.out.println(nextLog(responseData, i+2));
			                   break;
			                case 3:
                            	System.out.println("Action : Recharge Abonnement");
                            	break;
			                case 4:
			                	System.out.println("Action : Recharge Unité");
			                	System.out.println("Nombre d'unité recharger : " +nextLog(responseData, i+1));
                                break;
			                case 5:
			                	System.out.println("Action : Enlever Suspension");
			                	res = nextLog(responseData, i+1);
			                	switch ((int) res) {
			                		case 0:
			                			System.out.println("Carte actif");
			                			break;
									case 1:
										System.out.println("Carte suspendu");
										break;
									case 2:
										System.out.println("Carte bloqué");
										break;
								}
                                break;
			                case 6:
			                	System.out.println("Action : Débloquer Carte");
			                	res = nextLog(responseData, i+1);
			                	switch ((int) res) {
			                		case 0:
			                			System.out.println("Carte actif");
			                			break;
									case 1:
										System.out.println("Carte suspendu");
										break;
									case 2:
										System.out.println("Carte bloqué");
										break;
								}
                                break;
			                case 7:
			                	System.out.println("Action : Consulter Historique");
                                break;
                            case -1:
	                        	System.out.println("Action : Échec restitution d'un vélo");
	                        	break;
	                        case -2:
			                   System.out.println("Action : Échec intérroger la balance");
			                   break;
			                case -3:
                            	System.out.println("Action : Échec recharge Abonnement");
                            	break;
			                case -4:
			                	System.out.println("Action : Échec recharge Unité");
                                break;
			                case -5:
			                	System.out.println("Action : Échec enlever Suspension");
                                break;
			                case -6:
			                	System.out.println("Action : Échec débloquer Carte");
                                break;
			                case -7:
			                	System.out.println("Action : Échec consulter Historique");
                                break;
		                }
		            }
		           
		        } else {
		            System.out.println("Erreur inconnue (code : " + Integer.toHexString(apdu.getStatus()) + ").");
		        }
				break;
			case 9:
				fin = true;
				break;
			case 10:
				apdu.command[Apdu.INS] = MonApplet.INS_SUSPEND_TOI;
				cad.exchangeApdu(apdu);
				System.out.println("Status code: " + Integer.toHexString(apdu.getStatus() & 0xFFFF));
				if (apdu.getStatus() != 0x9000) {
					System.out.println("Erreur : status word different de 0x9000");
				} else {
					System.out.println("OK");
				}
				break;
			}
		}
		try {
			cad.powerDown();
		} catch (Exception e) {
			System.out.println("Erreur lors de l'envoi de la commande Powerdown");
			return;
		}

	}

	private static long createNextDayMidnightTimestamp(long timestampEmprunt) {
	    // Convertir le timestamp d'emprunt en date
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTimeInMillis(timestampEmprunt);
	    
	    // Ajouter un jour à la date de l'emprunt
	    calendar.add(Calendar.DAY_OF_MONTH, 1);
	    
	    // Réinitialiser l'heure à 1h du matin (1:00)
	    calendar.set(Calendar.HOUR_OF_DAY, 1);
	    calendar.set(Calendar.MINUTE, 0);
	    calendar.set(Calendar.SECOND, 0);
	    calendar.set(Calendar.MILLISECOND, 0);
	    calendar.set(Calendar.AM_PM, Calendar.AM);
	    
	    // Retourner le timestamp du lendemain à 1h du matin
	    return calendar.getTimeInMillis();
	}
	private static boolean isLeapYear(long year) {
	    // A year is a leap year if it is divisible by 4 but not by 100, unless it is divisible by 400
	    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
	}

	private static long createMidnightTimestampFromEmprunt(long timestampEmprunt) {
	    // Convertir le timestamp d'emprunt en date
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTimeInMillis(timestampEmprunt);
	    
	    // Réinitialiser l'heure à minuit (00:00)
	    calendar.set(Calendar.HOUR_OF_DAY, 24);
	    calendar.set(Calendar.MINUTE, 59);
	    calendar.set(Calendar.SECOND, 0);
	    calendar.set(Calendar.MILLISECOND, 0);
	    
	    // Retourner le timestamp de minuit du jour de l'emprunt
	    return calendar.getTimeInMillis();
	}
	

	public static long getCurrentYear() {
        long currentTimestamp = System.currentTimeMillis(); // Récupère le timestamp actuel
        return extractYear(currentTimestamp); // Utilise la méthode pour extraire l'année
    }

    // Fonction pour obtenir le mois actuel
    public static long getCurrentMonth() {
        long currentTimestamp = System.currentTimeMillis(); // Récupère le timestamp actuel
        return extractMonth(currentTimestamp); // Utilise la méthode pour extraire le mois
    }

    // Fonction pour extraire le mois à partir d'un timestamp
    public static long extractMonth(long timestamp) {
        // Constantes
        long millisecondsInDay = 86400000L; // 24h * 60 min * 60 sec * 1000 ms
        long millisecondsInYear = 365L * millisecondsInDay; // Année approximative (365 jours)

        // Calculer l'année
        long year = 1970 + (timestamp / millisecondsInYear);
        long dayOfYear = (timestamp / millisecondsInDay) % 365L; // Jour de l'année
        long month = 0;
        long remainingDays = dayOfYear;

        // Jours dans chaque mois pour une année non bissextile
        long[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        // Calcul du mois à partir du jour de l'année
        for (int i = 0; i < 12; i++) {
            if (remainingDays < daysInMonth[i]) {
                month = i;
                break;
            }
            remainingDays -= daysInMonth[i];
        }

        // Ajouter 1 au mois pour qu'il soit sur la base de 1 (janvier = 1, février = 2, etc.)
        month += 1;

        // Retourner le mois
        return month;
    }

    // Fonction pour extraire l'année à partir d'un timestamp
    public static long extractYear(long timestamp) {
        // Constantes
        long millisecondsInYear = 365L * 86400000L; // Année approximative (365 jours)

        // Calculer l'année
        long year = 1970 + (timestamp / millisecondsInYear);

        // Retourner l'année
        return year;
    }

    // Fonction pour comparer les mois et années récupérés depuis la carte avec la date actuelle
    public static boolean isCurrentMonthAndYearValid(long moisCarte, long anneeCarte) {
        // Récupérer le mois et l'année actuels
        long currentMonth = getCurrentMonth();
        long currentYear = getCurrentYear();

        // Comparer le mois et l'année
        if (moisCarte == currentMonth && anneeCarte == currentYear) {
            return true; // Si le mois et l'année sont les mêmes, l'abonnement est valide
        }
        return false;
    }

    public static long nextLog(byte responseData[], int i) {
    	long log = 0;
    	for (int j = 0; j < 8; j++) {
           log = (log << 8) | (responseData[i * 8 + j] & 0xFF); // Reconstituer le long à partir des octets
        }
    	return log;
    }
}
