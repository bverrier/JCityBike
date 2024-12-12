package card;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;


public class MonApplet extends Applet {
	
	// Constantes pour les types de vélos
    public static final short VELO_CLASSIQUE = 1;
    public static final short VELO_ELECTRIQUE = 2;
    public static final short BLOQUEE = 2;
    public static final short SUSPENDUE = 1;
    public static final short ACTIVE = 0;
    
	/* Constantes */
	private static final byte CLA_MONAPPLET = (byte) 0xB0;
	public static final byte INS_EMPRUNTER_VELO = 0x00;
	public static final byte INS_RENDRE_VELO = 0x01;
	public static final byte INS_INTERROGER_BALANCE = 0x02;
	public static final byte INS_RECHARGER_ABONNEMENT_BALANCE = 0x03;
	public static final byte INS_RECHARGER_UNITE_BALANCE = 0x04;
	public static final byte INS_ENLEVER_SUSPENSION_CARTE = 0x05;
	public static final byte INS_DEBLOQUER_CARTE = 0x06;
	public static final byte INS_CONSULTER_HISTORIQUE = 0x07;

    private static final short MAX_BALANCE = 32767;
    private static final short MAX_UNITE = 62;
    
	/* Attributs Externe a la carte*/
	private Date date; //date de la journée
	private short time; //nombre de minute écoulées depuis le début de la journée
	private short type; //1: vélo classique, 2: vélo electrique
	private short cost;
    private short IDBike;
    
    /* Attributs internes à la carte */
    private Abonnement dernierAbonnement; //La date du dernier abonnement
    private short nbElec; //Le nombre de vélos électriques empruntés au cours d’un mois valide d’abonnement
    private short balance;
    private byte codePIN;
    private byte codePUK;
    private short currentBikeID; //identifiant du vélo actuellement emprunté
    private byte etatCarte;
    private Date dateEmprunt;
 

	private MonApplet() {
		dernierAbonnement = new Abonnement();
		nbElec = 0;
		balance = 0;
		codePIN = (byte) 1234;
		codePUK = (byte) 12345;
		currentBikeID = 0;
		etatCarte = ACTIVE;
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
            default:
            	ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
}

	private void consulterHistorique(APDU apdu, byte[] buffer) {
		// TODO Auto-generated method stub
		
	}

	private void debloquerCarte(APDU apdu, byte[] buffer) {
		// TODO Auto-generated method stub
		
	}

	private void enleverSuspensionCarte(APDU apdu, byte[] buffer) {
		// TODO Auto-generated method stub
		
	}

	private void rechargerUniteBalance(APDU apdu, byte[] buffer) {
	    // Vérification de l'état de la carte
	    if (etatCarte != ACTIVE) {
	        ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
	    }

	    // Extraire P1 et P2
	    byte p1 = buffer[ISO7816.OFFSET_P1];
	    byte p2 = buffer[ISO7816.OFFSET_P2];

	    // Vérifier que P1 et P2 correspondent aux valeurs attendues
	    if (p1 != 0x00 || p2 != 0x00) {
	        ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
	    }

	    byte[] pinBuffer = new byte[4];
	    apdu.setIncomingAndReceive();

	    short pinLength = (short) (buffer[ISO7816.OFFSET_LC]);
	    if (pinLength != 4) {
	        ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
	    }

	    // Vérifier le code PIN
	    Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, pinBuffer, (short) 0, (short) 4);
	    if (!verifyPIN(pinBuffer)) {
	        ISOException.throwIt(ISO7816.SW_DATA_INVALID);
	    }

	    // Récupérer l'unité à ajouter (stockée après le code PIN)
	    short unite = (short) (buffer[ISO7816.OFFSET_CDATA + 4] & 0xFF);

	    // Vérifier que l'unité ne dépasse pas le maximum autorisé
	    if (unite > MAX_UNITE) {
	        ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
	    }

	    // Ajouter l'unité à la balance
	    balance += unite;

	    if (balance > MAX_BALANCE) {
	        balance = MAX_BALANCE;
	    }

	    // Retourner la nouvelle balance
	    buffer[0] = (byte) (balance >> 8);
	    buffer[1] = (byte) (balance & 0xFF);
	    apdu.setOutgoingAndSend((short) 0, (short) 2);
	}

	
	private boolean verifyPIN(byte[] pinBuffer) {
	    // Comparaison simple du code PIN entré avec le code PIN défini
	    for (short i = 0; i < 4; i++) {
	        if (pinBuffer[i] != (byte) ((codePIN >> (i * 8)) & 0xFF)) {
	            return false;
	        }
	    }
	    return true;
	}


	private void rechargerAbonnementBalance(APDU apdu, byte[] buffer) {
		if (dernierAbonnement.isValide(date)) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		
		
	}
	

	private void interrogerBalance(APDU apdu, byte[] buffer) {
		buffer[0] = (byte) balance;
		apdu.setOutgoingAndSend((short) 0, (short) 1);
	}

	private void rendreVelo(APDU apdu, byte[] buffer) {
		if (etatCarte != ACTIVE) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		
		if (currentBikeID == 0) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		
		Date dateRestitution = new Date();
		
		byte[] response = new byte[8];
		response[0] = (byte) (currentBikeID >> 8);
	    //byte[] dateBytes = dateToBytes(dateEmprunt);
        //System.arraycopy(dateBytes, 0, response, 2, dateBytes.length);

        // Envoyer la réponse à la borne
        apdu.setOutgoingAndSend((short) 0, (short) response.length);

        // Réinitialiser l'ID du vélo après la restitution
        currentBikeID = 0;
        
        
		
		
	}

	private void emprunterVelo(APDU apdu, byte[] buffer) {
		//check etat carte 
		/*if (etatCarte != ETAT_CARTE.ACTIVE) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
		//check si pas d'emprunt en cours
		if (currentBikeID != 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
		apdu.setIncomingAndReceive();
		date = buffer[ISO7816.OFFSET_CDATA];
		//check si un abonnement est valide du premier au dernier jour du mois
		if (!dernierAbonnement.isValide(date)) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		
		apdu.setIncomingAndReceive();
		cost = buffer[ISO7816.OFFSET_CDATA];
		
		
		if (balance < cost) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}

		apdu.setIncomingAndReceive();
		IDBike = buffer[ISO7816.OFFSET_CDATA];
		
		if (cost > 0) {
			currentBikeID = IDBike;
			balance -= cost;
		}*/
	}
		
		
	}