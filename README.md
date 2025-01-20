Pentru structura bancii am creat 4 clase noi ce contin toate datele acesteia, fiind organizate intr-o ierarhie.
In clasa Bank avem o lista de useri, apoi in clasa User avem o lista de conturi, iar in clasa Account avem o lista de 
carduri. De altfel, mai avem 2 clase ce au scopul de a stoca informatii utile pentru anumite comenzi:
1) Commerciant: un cont stocheaza o lista de elemente Commerciant, ce contine toate datele commerciant, dar si numarul de
tranzactii facute de un cont in legatura cu acestia. Mai exact, pentru fiecare comerciant dintr-un cont stocam numarul 
de tranzactii catre acel comerciant de contul respectiv, ajutandu-ne altfel pentru a crea un raport al unui cont de tip
business.
2) SplitPayment, ce ne ajuta sa stocam datele necesare pentru mai multe split payments in derulare. Mai exact, pentru
fiecare split payment in derulare, stocam o lista de clase SplitPayment, corespunzatoare fiecarui cont ce este implicat
in acesta. In aceasta clasa stocam informatii esentiale pentru payment-ul pe care trebuie sa-l faca fiecare cont,
cum ar fi suma de bani ce trebuie platita sau daca tranzactia a fost acceptata.
In clasa Bank tratam fiecare tip de comanda. Pentru fiecare comanda, avem nevoie de acces doar la datele
din ierarhia bancii (una din clasele Bank, User, Account, Card). La fiecare comanda, ne folosim de cate un handler
specific pentru fiecare din cele 4 clase. Acesti 4 handleri mostenesc clasa principala CommandHandler, ce este folosita
pentru a stoca datele comenzii si referinta clasei Bank, pentru acces la toate datele bancii, dar
si pentru crearea unor metode ce returneaza output-ul comenzii.

De altfel, ne folosim si de 8 clase de tip Visitor, ce implementeaza interfata CommandVisitor (cu 4 metode abstracte),
care contin ca parametrii una din 4 clase din ierarhia bancii si handler-ul comenzii specifice clasei.
Comenzile pot fi generalizate in mai multe categorii (spre exemplu avem clasa "Add", ce se ocupa cu adaugarea
fondurilor, adaugarea unui cont, sau adaugarea dobanzii, sau clasa "Payment" ce se ocupa cu transferul banilor
in alt cont, plata cu cardul, etc), astfel aproape toate clasele visitor se ocupa de mai multe comenzi prin
metodele visit, fiecare primind ca parametru clasa la care trebuie sa aiba acces pentru accesul la datele de care are
nevoie, dar si la handler-ul specific acesteia(pentru a avea acces la informatiile date in comanda). Astfel, fiecare
clasa din ierarhia bancii contine o metoda accept corespunzatoare ce trimite la metoda visit din clasa
specifica Visitor, avand ca parametru doare datele de care are nevoie pentru a procesa comanda. 1)*

Pentru a organiza mai bine codul, am creat clasa CommandVisitorFactory, ce defineste o interfață pentru crearea
claselor Visitor, astfel incat sa putem crea obiecte de tip Visitor fara a sti clasa exacta a acestora.
Analog si pentru handler, am creat clasa CommandHandlerFactory, ce defineste o interfață pentru crearea 
claselor de tip Handler, nefiind nevoie sa stim exact ce clasa specifica de handler trebuie sa cream pentru fiecare
comanda. 2)*

De altfel, pentru a ne asigura ca vom avea o singura instanta pentru clasa Bank, folosita pentru a stoca toate datele 
bancii, am folosit Singleton Pattern, adaugand metoda delete (incorect pentru accest pattern) doar pentru a sterge instanta
clasei Bank, in cazul in care vrem sa o recream cu alte date cand rulam alt test. De asemenea, am folosit Singleton Pattern si pentru
clasele CommandVisitorFactory si CommandHandlerFactory, deoarece nu avem nevoie decat de o singură instanta a acestora,
dar si pentru economisirea memoriei. 3)*

Nu in ultimul rand, folosim si design pattern-ul Builder, pentru a crea si configura clasele din ierarhia bancii
(User, Account, Card), eliminand supraincarcarea de constructori multipli, spre exemplu in cazul in care avem nevoie
de un cont de tip business, de tip savings, sau de classic sau de un user cu un plan anume, sau de un card de tip
OneTime. 4)*

Design Patterns folosite:
1) Visitor: Pentru procesarea comenzilor intr-un mod structurat, ocupandu-ne de mai multe timpuri de comenzi pentru 
fiecare clasa din ierarhia bancii.
2) Factory: Pentru simplificare crearii de obiecte de tip Visitor si Handler, fara a sti clasa exacta a acestora, astfel
putand adauga alte tipuri de comenzi intr-un mod accesibil.
3) Singleton Pattern: Anumite clase nu au nevoie de mai multe instante, cum ar fi clasele Factory sau clasa Bank.
4) Builder: Pentru a simplifica crearea unor diferite tipuri de conturi, carduri sau useri.

Radu Stoican 321CAb
