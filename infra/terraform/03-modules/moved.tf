moved {
  from = random_pet.greeter
  to   = module.greeting.random_pet.greeter
}

moved {
  from = local_file.hello
  to   = module.greeting.local_file.hello
}

moved {
  from = module.greeting
  to   = module.greeting["hello"]
}
