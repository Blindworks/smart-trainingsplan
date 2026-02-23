document.addEventListener("DOMContentLoaded", function () {
  const cards = document.querySelectorAll(".features-section .feature-card");

  if (!cards.length) {
    return;
  }

  cards.forEach((card) => card.classList.add("is-hidden"));

  if (!("IntersectionObserver" in window)) {
    cards.forEach((card) => {
      card.classList.remove("is-hidden");
      card.classList.add("is-visible");
    });
    return;
  }

  const observer = new IntersectionObserver(
    (entries, obs) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) {
          return;
        }

        entry.target.classList.remove("is-hidden");
        entry.target.classList.add("is-visible");
        obs.unobserve(entry.target);
      });
    },
    { threshold: 0.12 }
  );

  cards.forEach((card) => observer.observe(card));
});
