package card;

public class Date {
	private byte day;
	private byte month;
	private byte year;
	
	public Date(byte day, byte month, byte year) {
		this.day = day;
		this.month = month;
		this.year = year;
	}
	
	public Date() {
		//trouver la date actuelle, puis le début du mois et la fin du mois
		
	}
	
	public byte getDay() {
		return day;
	}
	
	public byte getMonth() {
		return month;
	}
	
	public byte getYear() {
		return year;
	}
	
	public void setDay(byte day) {
		this.day = day;
	}
	
	public void setMonth(byte month) {
		this.month = month;
	}
	
	public void setYear(byte year) {
		this.year = year;
	}
	
	public boolean equals(Date d) {
		return day == d.day && month == d.month && year == d.year;
	}
	
	public boolean isBefore(Date d) {
		if (year < d.year) {
			return true;
		} else if (year == d.year) {
			if (month < d.month) {
				return true;
			} else if (month == d.month) {
				return day < d.day;
			}
		}
		return false;
	}
	
	public boolean isAfter(Date d) {
		if (year > d.year) {
			return true;
		} else if (year == d.year) {
			if (month > d.month) {
				return true;
			} else if (month == d.month) {
				return day > d.day;
			}
		}
		return false;
	}

	public int compareTo(Date date) {
		if (isBefore(date)) {
			return -1;
		} else if (equals(date)) {
			return 0;
		} else {
			return 1;
		}
	}
	
}
