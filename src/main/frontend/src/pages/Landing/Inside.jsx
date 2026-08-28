/**
 * Who is on the other side of a candidature. A candidate is told which people
 * read their file and what each of them records.
 */
export default function Inside() {
  return (
    <section className="pubband inside">
      <div className="pubband__inner">
        <div className="section__head">
          <p className="section__eyebrow">De l'autre côté</p>
          <h2 className="section__title">Qui lit votre candidature</h2>
        </div>

        <div className="inside__list">
          <article className="inside__block">
            <h3 className="inside__title">Le recruteur</h3>
            <p className="inside__body">
              Il écrit l'offre et les traits qu'elle demande, lit votre candidature en
              présélection, fixe la date de vos entretiens, mène l'entretien final et prend la
              décision. Il ne voit que les offres qu'il a publiées et ce qui est arrivé par elles.
            </p>
          </article>

          <article className="inside__block">
            <h3 className="inside__title">L'expert technique</h3>
            <p className="inside__body">
              Le recruteur lui confie votre examen, à une date et une heure convenues. Il vous note
              sur les traits de l'offre, de zéro à cinq par demi points, et écrit son commentaire.
              Il ne voit que les examens qui lui ont été confiés.
            </p>
          </article>
        </div>
      </div>
    </section>
  );
}
