export function LoadingSkeleton() {
  return (
    <>
      {[0, 1, 2, 3].map((key) => (
        <section className="card" aria-hidden="true" key={key}>
          <div className="skeleton" style={{ height: '0.75em', width: '90px', marginBottom: '10px' }} />
          <div className="skeleton" style={{ height: '1.75em', width: '220px', marginTop: '6px', marginBottom: '10px' }} />
          <div className="skeleton" style={{ height: '1em', width: '60%' }} />
        </section>
      ))}
    </>
  );
}
