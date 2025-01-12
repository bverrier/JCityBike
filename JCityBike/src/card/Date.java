package card;

public class Date {
	private byte day;
	private byte month;
	private byte year;
	private short time; // time in minutes from midnight
	private long timestamp;
	
	
	
	public Date(long timestamp) {
        this.timestamp = timestamp;
		// Convertir le timestamp en secondes
        long seconds = timestamp / 1000;

        // Calculer l'année, le mois, le jour
        int daysSinceEpoch = (int) (seconds / 86400); // Nombre de jours depuis le 1er janvier 1970 (86400 secondes par jour)
        int fullYear = 1970;

        // Calculer l'année
        while (true) {
            int daysInYear = isLeapYear(fullYear) ? 366 : 365;
            if (daysSinceEpoch < daysInYear) break;
            daysSinceEpoch -= daysInYear;
            fullYear++;
        }

        // Stocker l'année sous forme de byte (utiliser les deux derniers chiffres de l'année)
        this.year = (byte) (fullYear % 100);

        // Calculer le mois
        int[] daysInMonth = getDaysInMonth(fullYear);
        this.month = 1;
        while (daysSinceEpoch >= daysInMonth[this.month - 1]) {
            daysSinceEpoch -= daysInMonth[this.month - 1];
            this.month++;
        }

        // Le jour
        this.day = (byte) (daysSinceEpoch + 1); // Le jour commence à 1

        // Calculer le nombre de minutes écoulées depuis le début de la journée
        int secondsInDay = (int) (seconds % 86400);  // Reste des secondes dans la journée
        this.time = (short) (secondsInDay / 60);  // Convertir les secondes en minutes
    }

    // Vérifier si une année est bissextile
    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    // Obtenir les jours de chaque mois pour une année donnée
    private int[] getDaysInMonth(int year) {
        if (isLeapYear(year)) {
            return new int[]{31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31}; // Année bissextile
        } else {
            return new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31}; // Année non-bissextile
        }
    }
	
	public Date(byte day, byte month, byte year, short time) {
		this.day = day;
		this.month = month;
		this.year = year;
		this.time = time;
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
	
	public short getTime() {
		return time;
	}
	
	public void setTime(short time) {
		this.time = time;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
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
