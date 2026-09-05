import React from 'react';

export const LandingWorks = () => {
  const steps = [
    {
      num: '01',
      title: 'Create / Register',
      desc: 'HR Create an Job and post Jobs. Candidates Signup and post their profiles.',
      numBg: 'bg-[#C63FC5]',
    },
    {
      num: '02',
      title: 'Find / Post Jobs',
      desc: 'HR Create an Job and post Jobs. Candidates Signup and post their profiles.',
      numBg: 'bg-[#6D3DF5]',
    },
    {
      num: '03',
      title: 'Apply & Review',
      desc: 'HR Create an Job and post Jobs. Candidates Signup and post their profiles.',
      numBg: 'bg-[#F56681]',
    },
    {
      num: '04',
      title: 'Assess & Interview',
      desc: 'HR Create an Job and post Jobs. Candidates Signup and post their profiles.',
      numBg: 'bg-[#FC9559]',
    },
    {
      num: '05',
      title: 'Offer & Verify',
      desc: 'HR Create an Job and post Jobs. Candidates Signup and post their profiles.',
      numBg: 'bg-[#C63FC5]',
    },
    {
      num: '06',
      title: 'Hire & Dashboard',
      desc: 'HR Create an Job and post Jobs. Candidates Signup and post their profiles.',
      numBg: 'bg-[#6D3DF5]',
    },
  ];

  return (
    <section id="how-it-works" className="py-16 md:py-20 px-6 max-w-7xl mx-auto">
      <div className="mx-auto px-2 sm:px-5">
        {/* Heading */}
        <div className="text-center">
          <h2 className="text-4xl md:text-5xl font-bold font-heading tracking-tight text-foreground">
            How <span className="gradient-text-brand">Hiring Works</span>
          </h2>

          <p className="mt-4 text-muted-foreground text-base md:text-md max-w-2xl mx-auto font-sans">
            Simple 6-step recruitment lifecycle from initial posting to final placement.
          </p>
        </div>

        {/* Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-12">
          {steps.map((step) => (
            <div
              key={step.num}
              className="bg-[#6D3DF5]/5 dark:bg-[#6D3DF5]/10 border border-dashed border-[#6D3DF5]/40 dark:border-[#6D3DF5]/60 rounded-3xl p-8 text-center glass hover-lift transition"
            >
              <div className={`w-11 h-11 rounded-full ${step.numBg} text-white font-bold text-xl flex items-center justify-center mx-auto shadow-md`}>
                {step.num}
              </div>

              <h3 className="text-2xl font-semibold font-heading mt-8 text-foreground">
                {step.title}
              </h3>

              <p className="mt-6 text-muted-foreground text-sm md:text-base leading-relaxed font-sans">
                {step.desc}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
