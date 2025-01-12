package card;

public class Abonnement {
    private long mois; // Date de début en millisecondes
    private long annee;   // Date de fin en millisecondes 
    
    // Constructeur avec dates en millisecondes
    public Abonnement(long mois, long annee) {
        this.mois = mois;
        this.annee = annee;
    }
    
    // Constructeur par défaut
    public Abonnement() {
        this(-1, -1);  // Initialisation avec 0 (dates de base, valable pour une carte JavaCard)
    }
    
    // Getter pour la date de début
    public long getMois() {
        return mois;
    }
    
    public long getAnnee() {
        return annee;
    }
    
    public void setMois(long mois) {
        this.mois = mois;
    }
    
    public void setAnnee(long annee) {
        this.annee = annee;
    }
    
    public long extractMonth(long timestamp) {
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
	
	public long extractYear(long timestamp) {
        // Constantes
        long millisecondsInYear = 365L * 86400000L; // Année approximative (365 jours)

        // Calculer l'année
        long year = 1970 + (timestamp / millisecondsInYear);

        // Retourner l'année
        return year;
    }
	
    
    public boolean isValide(long timestamp) {
    	long mois = extractMonth(timestamp);
        long annee = extractYear(timestamp);
    	
        if (this.annee == annee && this.mois <= mois) {
            return true;
        }
        
        if (mois == 12 && annee == this.annee + 1) {
            return true; 
        }
        
        return false; 
    }

}
