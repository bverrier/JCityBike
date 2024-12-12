package card;

public class Abonnement {
	private Date dateDebut;
	private Date dateFin;
	
	public Abonnement(Date dateDebut, Date dateFin) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}
	
	public Abonnement() {
		this(new Date(), new Date());
	}
	
	public Date getDateDebut() {
		return dateDebut;
	}
	
	public Date getDateFin() {
		return dateFin;
	}
	
	public void setDateDebut(Date dateDebut) {
		this.dateDebut = dateDebut;
	}
	
	public void setDateFin(Date dateFin) {
		this.dateFin = dateFin;
	}
	
	public boolean isValide(Date actuel) {
		return dateDebut.isAfter(actuel) && dateFin.isBefore(actuel);
	}
	
	
}
