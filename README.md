Pentru structura bancii am creat 5 clase noi ce contin toate datele acesteia, f>
In clasa Bank avem o lista de useri, apoi in clasa User avem o lista de conturi>
avem o lista de carduri.
In clasa Bank tratam fiecare tip de comanda. Pentru fiecare comanda, avem nevoi>
din ierarhia bancii (una din clasele Bank, User, Account, Card). La fiecare com>
specific pentru fiecare din cele 5 clase. Acesti 4 handleri mostenesc clasa pri>
pentru a stoca datele comenzii si referinta clasei Bank, pentru acces la toate >
si pentru crearea unor metode ce returneaza output-ul comenzii.
De altfel, ne folosim si de 6 clase de tip Visitor, ce implementeaza interfata >
care contin ca parametrii una din 4 clase din ierarhia bancii si handler-ul com>
Comenzile pot fi generalizate in mai multe categorii (spre exemplu avem clasa ">
fondurilor, adaugarea unui cont, sau adaugarea dobanzii, sau clasa "Payment" ce>
in alt cont, plata cu cardul, etc), astfel aproape toate clasele visitor se ocu>
metodele visit, fiecare primind ca parametru clasa la care trebuie sa aiba acce>
nevoie, dar si la handler-ul specific acesteia(pentru a avea acces la informati>
clasa din ierarhia bancii contine o metoda accept corespunzatoare ce trimite la>
specifica Visitor, avand ca parametru doare datele de care are nevoie pentru a >
Radu Stoican 321CAb
