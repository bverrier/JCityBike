package card;

import javacard.framework.APDU;
import javacard.framework.OwnerPIN;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;


public class MonApplet extends Applet {
	
	// Constantes pour les types de vélos
    public static final short VELO_CLASSIQUE = 1;
    public static final short VELO_ELECTRIQUE = 2;
    public static final short BLOQUEE = 2;
    public static final short SUSPENDUE = 1;
    public static final short ACTIVE = 0;
    
	/* Constantes */
    public final static byte CLA_MONAPPLET = (byte) 0xB0;
	public static final byte INS_EMPRUNTER_VELO = 0x00;
	public static final byte INS_RENDRE_VELO = 0x01;
	public static final byte INS_INTERROGER_BALANCE = 0x02;
	public static final byte INS_RECHARGER_ABONNEMENT_BALANCE = 0x03;
	public static final byte INS_RECHARGER_UNITE_BALANCE = 0x04;
	public static final byte INS_ENLEVER_SUSPENSION_CARTE = 0x05;
	public static final byte INS_DEBLOQUER_CARTE = 0x06;
	public static final byte INS_CONSULTER_HISTORIQUE = 0x07;
	public static final byte INS_SUSPEND_TOI= 0x08;
	public static final byte INS_ETAT_CARTE= 0x09;
	public static final byte INS_ETAT_ABONNEMENT= 0x0A;

    private static final short MAX_BALANCE = 32767;
    private static final short MAX_UNITE = 62;
    private static final short MAX_ESSAI = 3;
    
	/* Attributs Externe a la carte*/
	private byte day; //date de la journée
	private byte month; //mois de l’année	
	private byte year; //année
	private short time; //nombre de minute écoulées depuis le début de la journée
	private short type; //1: vélo classique, 2: vélo electrique
	private short cost;
    private short IDBike;
    private short TypeBike;

    /* Attributs internes à la carte */
    private Abonnement dernierAbonnement; //La date du dernier abonnement
    private short nbElec; //Le nombre de vélos électriques empruntés au cours d’un mois valide d’abonnement
    private short balance;
    private OwnerPIN codePIN;
    private OwnerPIN codePUK;
    private short currentBikeID; //identifiant du vélo actuellement emprunté
    private byte etatCarte;
    private Date dateEmprunt;
    
    private static final byte MAX_LOG_ENTRIES = 127;
    //Tableau qui stock les transactions
    private static final long[] logs = new long[MAX_LOG_ENTRIES];
    private static final byte MAX_TIME_LOG_ENTRIES = 127;
    //Tableau qui stock la date des transactions
    private static final long[] time_logs = new long[MAX_TIME_LOG_ENTRIES];
    private static short logIndex = 0;
    private final long DEFAULT_NEGATIVE_VALUE = -123;

    
    /* Constantes Erreurs */
    private static final short SW_INVALID_COST = 0x6A80;
    private static final short SW_INVALID_BIKE_TYPE = 0x6A81;
    private static final short SW_BIKE_NOT_AVAILABLE = 0x6A82;
	private static final short SW_CARD_BLOCKED = 0x6A83;   
	private static final short SW_CARD_SUSPEND = 0x6A84;   
	private static final short SW_TOO_LATE = 0x6A85;
	private static final short SW_ABONNEMENT_EXIST = 0x6A86;
	private static final short SW_INSUFFICE = 0x6A87;
	private static final short SW_PUK_INVALID = 0x6A88;
	private static final short SW_NOT_SUSPENDED = 0x6A89;
	private static final short SW_NOT_BLOCKED = 0x6A8A;
	private static final short SW_ETAT_CARTE_NOT_CORRECT = 0x6A8B;
	private static final short SW_PIN_INVALID = 0x6A8C;
	private static final short SW_RECORD_NOT_FOUND = 0x6A8D;

	private MonApplet() {
		dernierAbonnement = new Abonnement();
		nbElec = 0;
		balance = 00;
		codePIN = new OwnerPIN((byte) MAX_ESSAI, (byte) 4);
		codePIN.update(new byte[]{1, 2, 3, 4}, (short) 0, (byte) 4);
		codePUK = new OwnerPIN((byte) 3, (byte) 5);
		codePUK.update(new byte[]{1, 2, 3, 4, 5}, (short) 0, (byte) 5);
		currentBikeID = 0;
		etatCarte = ACTIVE;
		day = 0;
		month = 0;
		year = 0;
		time = 0;
		initializeLogs();
	}

	private void initializeLogs() {
	    for (int i = 0; i < logs.length; i++) {
	        logs[i] = DEFAULT_NEGATIVE_VALUE;
	    }
	    for (int i = 0; i < time_logs.length; i++) {
	        time_logs[i] = DEFAULT_NEGATIVE_VALUE;
	    }
	}

	public static void install(byte bArray[], short bOffset, byte bLength) throws ISOException {
		new MonApplet().register();
	}

	public void process(APDU apdu) throws ISOException {
		byte[] buffer = apdu.getBuffer();
		
		if (selectingApplet()) {return;}
		
		if (buffer[ISO7816.OFFSET_CLA] != CLA_MONAPPLET) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		
		switch (buffer[ISO7816.OFFSET_INS]) {
			case INS_EMPRUNTER_VELO:
                emprunterVelo(apdu, buffer);
                break;
            case INS_RENDRE_VELO:
                rendreVelo(apdu, buffer);
                break;
            case INS_INTERROGER_BALANCE:
                interrogerBalance(apdu, buffer);
                break;
            case INS_RECHARGER_ABONNEMENT_BALANCE:
                rechargerAbonnementBalance(apdu, buffer);
                break;
            case INS_RECHARGER_UNITE_BALANCE:
                rechargerUniteBalance(apdu, buffer);
                break;
            case INS_ENLEVER_SUSPENSION_CARTE:
                enleverSuspensionCarte(apdu, buffer);
                break;
            case INS_DEBLOQUER_CARTE:
                debloquerCarte(apdu, buffer);
                break;
            case INS_CONSULTER_HISTORIQUE:
                consulterHistorique(apdu, buffer);
                break;
            case INS_SUSPEND_TOI:
                suspendToi(apdu, buffer);
                break;
            case INS_ETAT_CARTE:
				etatCarte(apdu, buffer);
				break;
			case INS_ETAT_ABONNEMENT:
				etatAbonnement(apdu, buffer);
				break;
            default:
            	ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	private void etatAbonnement(APDU apdu, byte[] buffer) {
	    if (dernierAbonnement == null) {
	        ISOException.throwIt(ISO7816.SW_DATA_INVALID); // Code d'erreur si aucun abonnement n'est défini
	    }

	    long debut = dernierAbonnement.getMois();
	    long fin = dernierAbonnement.getAnnee();

	    // Conversion de "debut" (long) en tableau d'octets
	    buffer[0] = (byte) (debut >> 24);
	    buffer[1] = (byte) (debut >> 16);
	    buffer[2] = (byte) (debut >> 8);
	    buffer[3] = (byte) (debut);

	    // Conversion de "fin" (long) en tableau d'octets
	    buffer[4] = (byte) (fin >> 24);
	    buffer[5] = (byte) (fin >> 16);
	    buffer[6] = (byte) (fin >> 8);
	    buffer[7] = (byte) (fin);

	    // Transmission des 8 octets
	    apdu.setOutgoingAndSend((short) 0, (short) 8);
	}

	private void etatCarte(APDU apdu, byte[] buffer) {
		buffer[0] = etatCarte;
		apdu.setOutgoingAndSend((short) 0, (short) 1);
	}

	private void suspendToi(APDU apdu, byte[] buffer) {
		etatCarte = SUSPENDUE;
		ISOException.throwIt(SW_CARD_SUSPEND);
	}
	
	private void incrementLog(){
		if (logIndex+1 > MAX_LOG_ENTRIES) {
			logIndex = 0;
		} else {
			logIndex += 1;
		}
	}
	private void sauvegarderHistorique(long data, long time) {
		logs[logIndex] = data;
		time_logs[logIndex] = time;
		incrementLog();
	}

	private void consulterHistorique(APDU apdu, byte[] buffer) {
		if (etatCarte == BLOQUEE) {
			ISOException.throwIt(SW_CARD_BLOCKED);
		}
		if(etatCarte == SUSPENDUE) {
            ISOException.throwIt(SW_CARD_SUSPEND);
        }
		short le = apdu.setIncomingAndReceive();
		if (le != 5) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
		
		byte[] pukBuffer = new byte[5];
		pukBuffer[0] = buffer[ISO7816.OFFSET_CDATA];
		pukBuffer[1] = buffer[ISO7816.OFFSET_CDATA + 1];
		pukBuffer[2] = buffer[ISO7816.OFFSET_CDATA + 2];
		pukBuffer[3] = buffer[ISO7816.OFFSET_CDATA + 3];
		pukBuffer[4] = buffer[ISO7816.OFFSET_CDATA + 4];
		
		if (!codePUK.check(pukBuffer, (short) 0, (byte) 5)) {
			ISOException.throwIt(SW_PUK_INVALID);
		}
		
	    if (logs[0] == DEFAULT_NEGATIVE_VALUE) {
	        ISOException.throwIt(SW_RECORD_NOT_FOUND);
	    }
	    short length = 0;
	    //retourner le tableau logs
	    for (short i = 0; i < MAX_LOG_ENTRIES; i++) {
	        if (logs[i] != DEFAULT_NEGATIVE_VALUE) {
	            byte[] longBytes = new byte[8];
	            for (int j = 0; j < 8; j++) {
	                longBytes[j] = (byte) (logs[i] >> (56 - j * 8));
	            }
	            for (int j = 0; j < 8; j++) {
	                buffer[length] = longBytes[j];
	                length++;
	            }
	        }
	    }
	    apdu.setOutgoingAndSend((short) 0, length);
	}

	private void debloquerCarte(APDU apdu, byte[] buffer) {
		if (etatCarte != BLOQUEE) {
			ISOException.throwIt(SW_NOT_BLOCKED);
		}
		short le = apdu.setIncomingAndReceive();
		if (le != 5) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
		//Extraire le code PUK
		byte[] pukBuffer = new byte[5];
		pukBuffer[0] = buffer[ISO7816.OFFSET_CDATA];
		pukBuffer[1] = buffer[ISO7816.OFFSET_CDATA + 1];
		pukBuffer[2] = buffer[ISO7816.OFFSET_CDATA + 2];
		pukBuffer[3] = buffer[ISO7816.OFFSET_CDATA + 3];
		pukBuffer[4] = buffer[ISO7816.OFFSET_CDATA + 4];
		//Vérifier le code PUK
		if (codePUK.getTriesRemaining() == 0) {
			etatCarte = BLOQUEE;
			sauvegarderHistorique(-INS_DEBLOQUER_CARTE,0);
            sauvegarderHistorique(etatCarte, 0);
            sauvegarderHistorique(0, 0);
			ISOException.throwIt(SW_CARD_BLOCKED);
		}
		if (!codePUK.check(pukBuffer, (short) 0, (byte) 5)) {
			ISOException.throwIt(SW_PUK_INVALID);
		}else {
			codePUK.reset();
			codePIN.reset();
			etatCarte = ACTIVE;
			sauvegarderHistorique(INS_DEBLOQUER_CARTE,0);
            sauvegarderHistorique(etatCarte, 0);
            sauvegarderHistorique(0, 0);
			ISOException.throwIt(ISO7816.SW_NO_ERROR);
		}
	}

	private void enleverSuspensionCarte(APDU apdu, byte[] buffer) {
		if (etatCarte != SUSPENDUE) {
			ISOException.throwIt(SW_NOT_SUSPENDED);
		}
		short le = apdu.setIncomingAndReceive();
		if (le != 5) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
		//Extraire le code PUK
		byte[] pukBuffer = new byte[5];
		pukBuffer[0] = buffer[ISO7816.OFFSET_CDATA];
		pukBuffer[1] = buffer[ISO7816.OFFSET_CDATA + 1];
		pukBuffer[2] = buffer[ISO7816.OFFSET_CDATA + 2];
		pukBuffer[3] = buffer[ISO7816.OFFSET_CDATA + 3];
		pukBuffer[4] = buffer[ISO7816.OFFSET_CDATA + 4];
		//Vérifier le code PUK
		if (codePUK.getTriesRemaining() == 0) {
			etatCarte = BLOQUEE;
			sauvegarderHistorique(-INS_ENLEVER_SUSPENSION_CARTE,0);
            sauvegarderHistorique(etatCarte, 0);
            sauvegarderHistorique(0, 0);
			ISOException.throwIt(SW_CARD_BLOCKED);
		}
		if (!codePUK.check(pukBuffer, (short) 0, (byte) 5)) {
			ISOException.throwIt(SW_PUK_INVALID);
		}else {
			codePUK.reset();
			codePIN.reset();
			etatCarte = ACTIVE;
			sauvegarderHistorique(INS_ENLEVER_SUSPENSION_CARTE,0);
            sauvegarderHistorique(etatCarte, 0);
            sauvegarderHistorique(0, 0);
			ISOException.throwIt(ISO7816.SW_NO_ERROR);
		}
	}

	private void rechargerUniteBalance(APDU apdu, byte[] buffer) {
	    if (etatCarte != ACTIVE) {
	        ISOException.throwIt(SW_ETAT_CARTE_NOT_CORRECT);
	    }
	    byte p1 = buffer[ISO7816.OFFSET_P1];
	    byte p2 = buffer[ISO7816.OFFSET_P2];
	    if (p1 != 0x00 || p2 != 0x00) {
	        ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
	    }
		short le = apdu.setIncomingAndReceive();
    	if (le != 5) {  // Attendu 5 octets (4 pour PIN + 1 pour montant)
        	ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
    	}
	    byte[] pinBuffer = new byte[4];
	    pinBuffer[0] = buffer[ISO7816.OFFSET_CDATA];     
	    pinBuffer[1] = buffer[ISO7816.OFFSET_CDATA + 1]; 
	    pinBuffer[2] = buffer[ISO7816.OFFSET_CDATA + 2];
	    pinBuffer[3] = buffer[ISO7816.OFFSET_CDATA + 3];
	
	    if (!verifiePIN(pinBuffer, (short) 0, (byte) 4)) {
	        ISOException.throwIt(ISO7816.SW_DATA_INVALID);
	    }
	    // Récupérer l'unité à ajouter (stockée après le code PIN)
	    short unite = (short) buffer[ISO7816.OFFSET_CDATA + 4];

	    // Vérifier que l'unité ne dépasse pas le maximum autorisé
	    if (unite > MAX_UNITE) {
	    	sauvegarderHistorique(-INS_RECHARGER_UNITE_BALANCE,0);
	        sauvegarderHistorique(0, 0);
	        sauvegarderHistorique(0, 0);
	        ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
	    }
	    balance += unite;
	    
	    if (balance > MAX_BALANCE) {
	        balance = MAX_BALANCE;
	    }
	    buffer[0] = (byte) (balance >> 8);
	    buffer[1] = (byte) (balance & 0xFF);
	    sauvegarderHistorique(INS_RECHARGER_UNITE_BALANCE,0);
        sauvegarderHistorique(unite, 0);
        sauvegarderHistorique(0, 0);
	    apdu.setOutgoingAndSend((short) 0, (short) 2);
	}

	
	private boolean verifiePIN(byte[] pinBuffer, short offset, byte length) {
	    if (!codePIN.check(pinBuffer, offset, length)) {
	        // Si les tentatives max sont atteintes, bloquer la carte
	        if (codePIN.getTriesRemaining() == 0) {
	            etatCarte = BLOQUEE;
	            sauvegarderHistorique(INS_ETAT_CARTE,0);
	            sauvegarderHistorique(etatCarte, 0);
	            sauvegarderHistorique(0, 0);
	            ISOException.throwIt(SW_CARD_BLOCKED);
	        }
	        return false;
	    }
	    return true;
	}


	private void rechargerAbonnementBalance(APDU apdu, byte[] buffer) {
	    // Vérification de l'état de la carte
		if (etatCarte == SUSPENDUE) {
			ISOException.throwIt(SW_CARD_SUSPEND);
		}else if(etatCarte == BLOQUEE) {
			ISOException.throwIt(SW_CARD_BLOCKED);
		}
	
	    // Extraire P1 et P2
	    byte p1 = buffer[ISO7816.OFFSET_P1];
	    byte p2 = buffer[ISO7816.OFFSET_P2];
	
	    // Vérifier que P1 et P2 correspondent aux valeurs attendues
	    if (p1 != 0x00 || p2 != 0x00) {
	        ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
	    }
	
    	//Extraire le code PIN
  		byte[] pinBuffer = new byte[4];
  		pinBuffer[0] = buffer[ISO7816.OFFSET_CDATA];
  		pinBuffer[1] = buffer[ISO7816.OFFSET_CDATA + 1];
  		pinBuffer[2] = buffer[ISO7816.OFFSET_CDATA + 2];
  		pinBuffer[3] = buffer[ISO7816.OFFSET_CDATA + 3];
  		
  		//Vérifier le code PIN
  		if (codePIN.getTriesRemaining() == 0) {
  			etatCarte = BLOQUEE;
  			ISOException.throwIt(SW_CARD_BLOCKED);
  		}
  		if (!codePIN.check(pinBuffer, (short) 0, (byte) 4)) {
  			ISOException.throwIt(SW_PIN_INVALID);
  		}
	    
	    // Extraire la date envoyée en millisecondes
  		long dateAbonnement = 0;
        for (short i = 0; i < 8; i++) {
            dateAbonnement = (dateAbonnement << 8) | (buffer[ISO7816.OFFSET_CDATA + 4 + i] & 0xFF);
        }
	
        long month = dernierAbonnement.extractMonth(dateAbonnement);
        long year = dernierAbonnement.extractYear(dateAbonnement);
	    // Si aucun abonnement n'existe ou si l'abonnement est expiré, on en crée un nouveau
	    if (!dernierAbonnement.isValide(dateAbonnement)) {
	        sauvegarderHistorique(INS_RECHARGER_ABONNEMENT_BALANCE, dateAbonnement);
	        sauvegarderHistorique(0, 0);
	        sauvegarderHistorique(0, 0);
	        dernierAbonnement = new Abonnement(month, year);
	        ISOException.throwIt(ISO7816.SW_NO_ERROR);
	    } else {
	    	sauvegarderHistorique(-INS_RECHARGER_ABONNEMENT_BALANCE, dateAbonnement);
	    	sauvegarderHistorique(0, 0);
	    	sauvegarderHistorique(0, 0);
	        ISOException.throwIt(SW_ABONNEMENT_EXIST);
	    }
	}
	
	public static long getEndOfMonth(long timestamp) {
        // Obtenir l'année, le mois, et le jour à partir du timestamp
        short year = (short) (timestamp / (1000L * 60L * 60L * 24L * 365L));
        short month = (short) ((timestamp / (1000L * 60L * 60L * 24L * 30L)) % 12);
        short day = (short) ((timestamp / (1000L * 60L * 60L * 24L)) % 30);
        short daysInMonth = (short) (30);
        if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
            daysInMonth = 31;
        } else if (month == 2) {
            // Vérifier si l'année est bissextile pour février
            if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                daysInMonth = 29;
            } else {
                daysInMonth = 28;
            }
        }
        long endOfMonthTimestamp = timestamp + (daysInMonth - day) * 1000L * 60L * 60L * 24L;
        return endOfMonthTimestamp;
    }

	private void interrogerBalance(APDU apdu, byte[] buffer) {
		if (etatCarte == SUSPENDUE) {
			ISOException.throwIt(SW_CARD_SUSPEND);
		}else if(etatCarte == BLOQUEE) {
			ISOException.throwIt(SW_CARD_BLOCKED);
		}
		buffer[0] = (byte) (balance >> 8);
    	buffer[1] = (byte) (balance & 0xFF);
    	long value = dernierAbonnement.getMois();
    	for (short i = 7; i >= 0; i--) {
            buffer[(short) (2 + i)] = (byte) (value & 0xFF);
            value >>= 8; 
        }
    	long value2 = dernierAbonnement.getAnnee();
    	for (short i = 7; i >= 0; i--) {
            buffer[(short) (10 + i)] = (byte) (value2 & 0xFF);
            value2 >>= 8; 
        }
    	sauvegarderHistorique(INS_INTERROGER_BALANCE, 0);
    	sauvegarderHistorique(balance, 0);
    	sauvegarderHistorique(0, 0);
    	apdu.setOutgoingAndSend((short) 0, (short) 18);
	}
	
	private void rendreVelo(APDU apdu, byte[] buffer) {
		if (etatCarte == SUSPENDUE) {
			ISOException.throwIt(SW_CARD_SUSPEND);
		}else if(etatCarte == BLOQUEE) {
			ISOException.throwIt(SW_CARD_BLOCKED);
		}
		// Vérifier que le vélo est bien emprunté
	    if (currentBikeID == 0) {
	    	sauvegarderHistorique(-INS_RENDRE_VELO, dateEmprunt.getTimestamp());
	    	sauvegarderHistorique(0, 0);
	    	sauvegarderHistorique(0, 0);
	        ISOException.throwIt(SW_BIKE_NOT_AVAILABLE); // Aucun vélo n'est actuellement emprunté
	    } else {
		    byte[] dataEnvoi = apdu.getBuffer();
		    dataEnvoi[0] = (byte) (currentBikeID >> 8);
		    dataEnvoi[1] = (byte) (currentBikeID & 0xFF);
		    dataEnvoi[2] = (byte) (dateEmprunt.getTimestamp() >> 56);
		    dataEnvoi[3] = (byte) (dateEmprunt.getTimestamp() >> 48);
		    dataEnvoi[4] = (byte) (dateEmprunt.getTimestamp() >> 40);
		    dataEnvoi[5] = (byte) (dateEmprunt.getTimestamp() >> 32);
	    	dataEnvoi[6] = (byte) (dateEmprunt.getTimestamp() >> 24);
	        dataEnvoi[7] = (byte) (dateEmprunt.getTimestamp() >> 16);	
	        dataEnvoi[8] = (byte) (dateEmprunt.getTimestamp() >> 8);
	        dataEnvoi[9] = (byte) (dateEmprunt.getTimestamp() & 0xFF);
		    apdu.setOutgoingAndSend((short) 0, (short) 10);
		    sauvegarderHistorique(INS_RENDRE_VELO, dateEmprunt.getTimestamp());
		    sauvegarderHistorique(0, 0);
		    sauvegarderHistorique(0, 0);
			currentBikeID = 0;
		}
	}
	
	private void emprunterVelo(APDU apdu, byte[] buffer) {
		if (etatCarte == SUSPENDUE) {
			ISOException.throwIt(SW_CARD_SUSPEND);
		}else if(etatCarte == BLOQUEE) {
			ISOException.throwIt(SW_CARD_BLOCKED);
		}
		
		if (currentBikeID != 0) {
			sauvegarderHistorique(-INS_EMPRUNTER_VELO, 0);
	    	sauvegarderHistorique(0, 0);
	    	sauvegarderHistorique(0, 0);
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		
		byte[] dataIn = apdu.getBuffer();
	    long timestamp = 0;
	    for (int i = 0; i < 8; i++) {
	        timestamp = (timestamp << 8) | (dataIn[ISO7816.OFFSET_CDATA + i] & 0xFF);
	    }
	    TypeBike = (short) (dataIn[ISO7816.OFFSET_CDATA + 8] & 0xFF);
	    IDBike = (short) (dataIn[ISO7816.OFFSET_CDATA + 9] << 8 | dataIn[ISO7816.OFFSET_CDATA + 10] & 0xFF);
	    cost = (short) (buffer[ISO7816.OFFSET_CDATA + 11] << 8 | buffer[ISO7816.OFFSET_CDATA+ 12] & 0xFF);
	    // Vérification de l'abonnement et des conditions d'emprunt
	    if (TypeBike == 1) { // Vélo classique
	        if (!dernierAbonnement.isValide(timestamp)) {
	            if (cost != 1) {
	                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
	            }
	        }
	    } else if (TypeBike == 2) { // Vélo électrique
	        if (dernierAbonnement.isValide(timestamp) && nbElec < 5) {
	            if (cost != 0) {
	                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
	            }
	            nbElec++;
	        } else {
	            if (cost != 4) {
	                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
	            }
	        }
	    }
	    // Vérifier si le solde est suffisant
	    if (balance < cost) {
	        ISOException.throwIt(SW_INSUFFICE);
	    }
	    balance -= cost;
	    currentBikeID = IDBike;
	    dateEmprunt = new Date(timestamp);
	    sauvegarderHistorique(INS_EMPRUNTER_VELO, 0);
	    sauvegarderHistorique(currentBikeID, 0);
	    sauvegarderHistorique(cost, 0);	    
	    apdu.setOutgoing();
	    apdu.setOutgoingLength((short) 0);

	}
}